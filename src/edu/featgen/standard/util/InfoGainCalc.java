package edu.featgen.standard.util;

import edu.featgen.def.Document;
import edu.wiki.util.Tuple;
import gnu.trove.TLongObjectHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class InfoGainCalc implements WeakClassifierForAdaBoostFS, FeatureRelevanceMeasure{
	public static double PRECISION_INV = Math.pow(10, 6);

	// holds expected sample-per-label counts. We can use this to calculate
	// how many samples with missing values we had
	double[] expectedWeightPerLabelHint = null;
	
	// counts how many samples we'v seen for each label
	double[] seenWeightPerLabel = null;

	// number of possible labels
	int nLabels;
	boolean recursive;
	EstimationEnum est;
	
	// Holds the selected cut points
	Set<Long> cutpoints = new HashSet<>();
	Long mainCutPoint = null;
	
	List<ValueT> allSamplesVec;
	List<Long> intervalCutpoints;
	int[] intervalClassification;
	
	TLongObjectHashMap<ValueT> allSamples = new TLongObjectHashMap<>();
	String featureName;
	
	public InfoGainCalc(int nLabels, String featureName){
		this(nLabels, true, featureName, EstimationEnum.INFO_GAIN);
	}
	
	public InfoGainCalc(int nLabels, boolean recursive, String featureName, EstimationEnum est){
		this.nLabels = nLabels;
		seenWeightPerLabel = new double[nLabels+1];
		this.recursive = recursive;
		this.featureName = featureName;
		this.est = est;
	}

	public void setExpectedWeightPerLabelHint(double[] expectedWeightPerLabelHint){
		this.expectedWeightPerLabelHint = expectedWeightPerLabelHint;
	}
	

	public void addSample(int classId, double value){
		addSample(classId, value, 1);
	}

	public void addSample(int classId, double value, double weight){
		if (value * PRECISION_INV >= (double)Long.MAX_VALUE * (double)nLabels){
			throw new RuntimeException("We do not support such high attribute "
					+ "values, try normalizing");
		}
		
		// Round the value
		long lvalue = (long)(value * PRECISION_INV);

		// add to samples
		long key = classId + lvalue * nLabels;
		ValueT valueT = allSamples.get(key);
		if (valueT == null){
			valueT = new ValueT(classId, lvalue, 0);
			allSamples.put(key, valueT);
		}
		
		valueT.weight+=weight;
		seenWeightPerLabel[nLabels]+=weight;
		seenWeightPerLabel[classId]+=weight;
	}
	
	@Override
	public double estimateRelevance(){
		cutpoints.clear();

		accountForMissingValues();

		allSamplesVec = new ArrayList<>(allSamples.size());
		allSamples.forEachValue((v)->allSamplesVec.add(v));
		
		Collections.sort(allSamplesVec, (t1,t2)->Long.compare(t1.value, t2.value));

		discretisize(allSamplesVec, seenWeightPerLabel);
		
		if (est == EstimationEnum.POS){
			return getPurityWhereExist();
		} else if (est == EstimationEnum.NEG){
			return 1 - getPurityWhereExist();
		} else {
			double entropyPre = entropy(seenWeightPerLabel, null, true, -1);
			double entropyAfter = entropyDiscretisized(allSamplesVec, cutpoints, -1);
			if (Double.compare(entropyPre, entropyAfter - 0.0000001) < 0){
				// sanity check
				throw new RuntimeException("bug!");
			}
			return entropyPre - entropyAfter;
		}
	}	

	/**
	 * this method works only when recursive=false and there are only two labels
	 * (assumed 0-negative, 1-positive)
	 * Define purity as % of positive examples.
	 * This outputs purity(post) - purity(prior)
	 * Where purity(prior)=positive/total
	 * purity(post) = positive/total of examples with value > then cutpoint
	 * 
	 * Supposed to give a simple indication of how much this feature indicates positive/negative
	 * examples
	 * 
	 * TODO: This is not actually what i'm doing
	 */
	public double getPurityWhereExist(){
		if(nLabels != 2){
			throw new RuntimeException("Only works when 2 possible labels");
		}
		// with this weighting for positive, the prior will always be 50% 
		double positiveW = expectedWeightPerLabelHint[0] / expectedWeightPerLabelHint[1];
		
		if(mainCutPoint == null){
			// no separation
			return 0.5;
		}
		
		double posCount = allSamplesVec.stream()
				.mapToDouble((s)->s.value >= mainCutPoint && s.classId == 1 ? s.weight : 0).sum();
		double negCount = allSamplesVec.stream()
				.mapToDouble((s)->s.value >= mainCutPoint && s.classId == 0 ? s.weight : 0).sum();
		double posW = posCount * positiveW / (posCount * positiveW + negCount);
		return posW;
	}
	
	@Override
	public double estimateRelevanceForLabel(int label){
		double entropyPre = entropy(seenWeightPerLabel, null, true, label);
		double entropyAfter = entropyDiscretisized(allSamplesVec, cutpoints, label);
		return entropyPre - entropyAfter;
	}

	public double entropyDiscretisized(List<ValueT> samples, Set<Long> cutpoints, int label){
		double ent = 0;
			
		List<Long> sortedCutPoints = new ArrayList<>(cutpoints);
		Collections.sort(sortedCutPoints, (t1,t2)->Long.compare(t1, t2));

		long cutpoint = sortedCutPoints.size() > 0 ? sortedCutPoints.get(0) : Long.MAX_VALUE;
		
		double[] weights = new double[nLabels+1];
		
		int cutpointIdx = 0;
		int lastIdx = 0;
		int idx = 0;
		double totalW = 0;
		while(idx < samples.size()){
			if(samples.get(idx).value > cutpoint){
				double e = entropy(weights, null, true, label);
				ent += e * weights[nLabels];
				lastIdx = idx;
				cutpointIdx++;
				cutpoint = sortedCutPoints.size() > cutpointIdx ? 
						sortedCutPoints.get(cutpointIdx) : Long.MAX_VALUE;
				for(int i = 0; i < weights.length; i++){
					weights[i] = 0;
				}
			}
			weights[samples.get(idx).classId] += samples.get(idx).weight;
			weights[nLabels] += samples.get(idx).weight;
			totalW += samples.get(idx).weight;
			idx++; 
		}
		if(idx > lastIdx){
			double e = entropy(weights, null, true, label);
			ent += e * weights[nLabels];
		}
		return ent / totalW;
	}
	
	// if we have a hint as to how many samples we expect for each label, and
	// we actually saw less samples then expected - we can assume these are due
	// to samples with missing values and add them as zeros (as this is our default
	// missing value)
	protected void accountForMissingValues(){
		if(expectedWeightPerLabelHint != null){
			for(int i = 0; i < nLabels; i++){
				if (seenWeightPerLabel[i] < expectedWeightPerLabelHint[i]){
					addSample(i, 0, expectedWeightPerLabelHint[i] - seenWeightPerLabel[i]);
				}
			}
		}
	}

	
	protected void discretisize(List<ValueT> samples, double[] totalCounts) {
		// Calculate Gain(A,T,S) > (log2(N-1) + delta(A,T,S)) / N
		// delta(A,T,S) = log2(3^k - 2) - [k*Ent(S) - k1*Ent(S1) - k2 * Ent(S2)]
		
		// S1,S2 - the partitions
		// k = total n of classes, k1 = classes in S1, k2 = classes in S2
		// Gain(A,T,S) = Ent(S) - (|S1|Ent(S1) + |S2|Ent(S2)) / N
		
		if (samples.size() < 2){
			// nothing to do
			return;
		}

		double entropyS = entropy(totalCounts, null, true, -1);
		double[] weights = new double[nLabels+1];
		double bestGain = -1;
		int bestSplit = -1; // means split between bestSplit and bestSplit+1
		double bestEntropyS1 = 0;
		double bestEntropyS2 = 0;

		int i = 0;
		while(i < samples.size()-1){
			// update counts
			updateCounts(weights, samples.get(i));
			
			// each value can have multiple entries, if samples with differing
			// labels are mapped to it
			boolean lastValueNotPure = false;
			long curValue = samples.get(i).value;
			int lastClassId = samples.get(i).classId;
			while(i+1 < samples.size() && samples.get(i+1).value == curValue){
				i++;
				lastValueNotPure = true;
				updateCounts(weights, samples.get(i));
			}

			// if there are adjacent two values with same class for all samples mapped
			// to these values, then a cut point can never be put in the middle
			if (!lastValueNotPure && samples.get(i).classId == lastClassId){
				// make sure the current value doesn't have more then one class
				if (i+1 < samples.size() && samples.get(i+1).value > samples.get(i+1).value){
					continue;
				}
			}
			
			// nothing to check
			if (i == samples.size()-1){
				break;
			}
			
			double gain = infoGain(weights, totalCounts, entropyS);
			if(gain > bestGain){
				bestSplit = i;
				bestGain = gain;
			}			
			i++;
		}

		if (bestSplit == -1){
			// there weren't any possible cut points (all samples are actually belonging
			// to same value due to quantization
			return;
		}

		if(cutpoints.isEmpty() || bestGain > getMDLPCriterion(samples,bestSplit,entropyS, bestEntropyS1, bestEntropyS2, totalCounts[nLabels])){
			if(cutpoints.isEmpty()){
				mainCutPoint = (samples.get(bestSplit).value + samples.get(bestSplit+1).value)/2;
			}
			// This is a good split, add it and continue
			cutpoints.add((samples.get(bestSplit).value + samples.get(bestSplit+1).value)/2);
			if(recursive){
				discretisize(samples.subList(0, bestSplit+1), weights);
				discretisize(samples.subList(bestSplit+1, samples.size()), weights);
			}
			return;
		} else {
			// cut point is not usefull
			return;
		}
		
	}
	
	protected double getMDLPCriterion(
			List<ValueT> samples, 
			int bestSplit, 
			double entropyS,
			double bestEntropyS1,
			double bestEntropyS2,
			double W){
		// get number of classes in total and in either size of the split
		double k = samples.stream().map(e -> e.classId).collect(Collectors.toSet()).size();
		double k1 = samples.subList(0, bestSplit).stream().map(e -> e.classId).collect(Collectors.toSet()).size();
		double k2 = samples.subList(bestSplit, samples.size()).stream().map(e -> e.classId).collect(Collectors.toSet()).size();

		// MDLP criterion from Fayyad & Irani
		// delta(A,T,S) = log2(3^k - 2) - [k*Ent(S) - k1*Ent(S1) - k2 * Ent(S2)]
		double delta = log2(Math.pow(3, k) - 2) - (k * entropyS - k1 * bestEntropyS1 - k2 * bestEntropyS2);
		return (log2(W-1) + delta) / W;
	}
	protected void updateCounts(double[] counts, ValueT v){
		counts[v.classId]+=v.weight;
		counts[nLabels] += v.weight;
	}

	public double infoGain(double[] weights, double[] totalWeights, double entropyS){
		double entropyS1 = entropy(weights, totalWeights, true, -1);
		double wS1 = weights[nLabels];
		double entropyS2 = entropy(weights, totalWeights, false, -1);
		double wS2 = totalWeights[nLabels] - weights[nLabels];
		
		return entropyS - (entropyS1 * wS1 + entropyS2 * (wS2)) / totalWeights[nLabels];
	}

	public double entropy(double[] weights, int label){
		return entropy(weights, null, true, label);
	}

	protected static double XlogX(double x) {
	    return x * log2(x);
	}
	protected static double log2(double x) {
	    return Math.log(x) / Math.log(2);
	}

	public double entropy(double[] weights, double[] totalWeights, boolean left, int label){
		double W = left ? weights[nLabels] : totalWeights[nLabels] - weights[nLabels];

		double ent = 0;
		for(int i = 0; i < nLabels; i++){
			if(weights[i] > 0 && (label < 0 || label == i)){
				ent -= XlogX(left ? weights[i] : totalWeights[i] - weights[i]);
			}
		}
		if(label < 0){
			ent += XlogX(W);
		}else{
			double w = left ? weights[label] : totalWeights[label] - weights[label];
			ent += w * log2(W);
		}
		ent /= (double)W;
		
		return ent;
	}

	public void prepareClassifier(){
		if(nLabels > 2){
			// Not a problem to implement but no use right now
			throw new RuntimeException("Cannot handle non binary");
		}
		intervalCutpoints = cutpoints.stream().sorted().collect(Collectors.toList());
		intervalCutpoints.sort((a,b)->a.compareTo(b));
		long[] intervalClassificationD = new long[cutpoints.size()+1];
		int interval = 0;
		
		for(ValueT t : allSamplesVec){
			// TODO: Binary search!!
			while(interval < intervalCutpoints.size() && 
					t.value > intervalCutpoints.get(interval)){
				interval++;
			}
			
			intervalClassificationD[interval] += t.classId > 0 ? t.weight : - t.weight;
		}
		intervalClassification = new int[cutpoints.size()+1];
		for(int i = 0; i < intervalClassification.length; i++){
			intervalClassification[i] = intervalClassificationD[i] > 0 ? 1 : 0;
		}
	}
	
	protected class KeyT{
		int classId;
		long value;
		protected KeyT(int classId, long value){
			this.classId = classId;
			this.value = value;
		}
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Tuple)){
				return false;
			}
			KeyT t = (KeyT)obj;
			return classId == t.classId && value == t.value;
		}
	  
		@Override
		public int hashCode() {
			return classId * 257 + (int)value * 137;
		}
	  
		@Override
		public String toString() {
			return new StringBuffer().append("<").append(classId).append(",").append(value).append(">").toString();
		}
	}
	protected class ValueT{
		int classId;
		long value;
		double weight;
		protected ValueT(int classId, long value, double weight){
			this.classId = classId;
			this.value = value;
			this.weight = weight;
		}
		
		public String toString(){
			return "<" + value + "," + classId + "," + weight + ">";
		}
	}
	
	@Override
	public void buildClasifier() {
		estimateRelevance();
		prepareClassifier();
	}

	@Override
	public void addSample(int label, Document doc, double weight) {
		// TODO Auto-generated method stub
		addSample(label,
				doc.getFeature(featureName) == null ? 0 : doc.getFeature(featureName).doubleValue(),
				weight);
	}
	

	@Override
	public int classify(Document doc){
		long v = doc.getFeature(featureName) == null ? 0 : (long)(doc.getFeature(featureName).doubleValue() * PRECISION_INV);
		if(intervalCutpoints.size() == 0 || v < intervalCutpoints.get(0)){
			return intervalClassification[0];
		} else if (v >= intervalCutpoints.get(intervalCutpoints.size()-1)){
			return intervalClassification[intervalCutpoints.size()];
		}
		
		int i = Collections.binarySearch(intervalCutpoints, v);
		
		return i >= 0 ? intervalClassification[i] :
			intervalClassification[-i - 1];
	}
	
	public enum EstimationEnum{
		INFO_GAIN, POS, NEG;
	}
}
