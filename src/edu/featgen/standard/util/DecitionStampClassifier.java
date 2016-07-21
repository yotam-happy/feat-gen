package edu.featgen.standard.util;

import edu.featgen.def.Document;
import gnu.trove.TLongObjectHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DecitionStampClassifier implements WeakClassifierForAdaBoostFS, FeatureRelevanceMeasure{
	public static double PRECISION_INV = Math.pow(10, 6);
	
	// holds expected sample-per-label counts. We can use this to calculate
	// how many samples with missing values we had
	double[] expectedWeightPerLabelHint = null;
	
	// counts how many samples we'v seen for each label
	double[] seenWeightPerLabel = null;

	TLongObjectHashMap<ValueT> allSamples = new TLongObjectHashMap<>();
	List<ValueT> allSamplesVec;
	long cutPoint;
	boolean isHighRangePositive; // is >cutPoint positive or negative?
	String featureName;

	public DecitionStampClassifier(String featureName){
		seenWeightPerLabel = new double[3];
		this.featureName = featureName;
	}

	public void setExpectedWeightPerLabelHint(double[] expectedWeightPerLabelHint){
		this.expectedWeightPerLabelHint = expectedWeightPerLabelHint;
	}

	@Override
	public void addSample(int label, double value) {
		addSample(label, value, 1);
	}

	@Override
	public void addSample(int label, double value, double weight){
		if (value * PRECISION_INV >= (double)Long.MAX_VALUE * 2){
			throw new RuntimeException("We do not support such high attribute "
					+ "values, try normalizing");
		}
		
		// Round the value
		long lvalue = (long)(value * PRECISION_INV);
		boolean positive = label != 0;
		// add to samples
		long key = (positive ? 1 : 0) + lvalue * 2;
		ValueT valueT = allSamples.get(key);
		if (valueT == null){
			valueT = new ValueT(positive, lvalue, 0);
			allSamples.put(key, valueT);
		}
		
		valueT.weight+=weight;
		seenWeightPerLabel[2]+=weight;
		seenWeightPerLabel[positive ? 1 : 0]+=weight;
		
	}

	// if we have a hint as to how many samples we expect for each label, and
	// we actually saw less samples then expected - we can assume these are due
	// to samples with missing values and add them as zeros (as this is our default
	// missing value)
	protected void accountForMissingValues(){
		if(expectedWeightPerLabelHint != null){
			if (seenWeightPerLabel[0] < expectedWeightPerLabelHint[0]){
				addSample(0, 0, expectedWeightPerLabelHint[0] - seenWeightPerLabel[0]);
			}
			if (seenWeightPerLabel[1] < expectedWeightPerLabelHint[1]){
				addSample(1, 0, expectedWeightPerLabelHint[1] - seenWeightPerLabel[1]);
			}
		}
	}
	
	@Override
	public void buildClasifier() {
		estimateRelevance();
		
		// release memory
		expectedWeightPerLabelHint = null;
		seenWeightPerLabel = null;
		allSamples = null;
		allSamplesVec = null;
	}

	@Override
	public double estimateRelevance(){
		accountForMissingValues();

		allSamplesVec = new ArrayList<>(allSamples.size());
		allSamples.forEachValue((v)->allSamplesVec.add(v));
		Collections.sort(allSamplesVec, (t1,t2)->Double.compare(t1.value, t2.value));
		
		double leftPositive = 0;
		double leftNegative = 0;
		double rightPositive = seenWeightPerLabel[1];
		double rightNegative = seenWeightPerLabel[0];
		
		long lastValue = allSamplesVec.get(0).value;
		
		long bestCutpoint = Long.MIN_VALUE;
		double bestLoss = Double.POSITIVE_INFINITY;
		boolean bestCutpointIsHighRangePositive = false;
		// get cutpoint with minimal loss
		for(int i = 0; i < allSamplesVec.size() + 1; i++){
			if(i == allSamplesVec.size() || 
					allSamplesVec.get(i).value > lastValue){
				boolean currentIsHighRangePositive = 
						rightNegative + leftPositive < rightPositive + leftNegative;
				double currentLoss = bestCutpointIsHighRangePositive ? 
						rightNegative + leftPositive : rightPositive + leftNegative;
				if(currentLoss < bestLoss){
					bestLoss = currentLoss;
					bestCutpoint = i == allSamplesVec.size() ?
							Long.MAX_VALUE :
							(lastValue + allSamplesVec.get(i).value) / 2;
					bestCutpointIsHighRangePositive = currentIsHighRangePositive;
				}
				if(i == allSamplesVec.size()){
					continue;
				}
			}
			if(allSamplesVec.get(i).positive){
				leftPositive += allSamplesVec.get(i).weight; 
				rightPositive -= allSamplesVec.get(i).weight; 
			} else {
				leftNegative += allSamplesVec.get(i).weight; 
				rightNegative -= allSamplesVec.get(i).weight; 
			}
			lastValue = allSamplesVec.get(i).value;
		}
		cutPoint = bestCutpoint;
		isHighRangePositive = bestCutpointIsHighRangePositive;
		
		return getInfoGain();
	}
	
	protected double entropy(double positiveW, double negativeW){
		double totalW = positiveW + negativeW;
		return (XlogX(totalW) - (XlogX(positiveW) + XlogX(negativeW))) / totalW;
	}
	
	protected double getInfoGain(){
		double priorEnt = entropy(seenWeightPerLabel[1], seenWeightPerLabel[0]);

		// find weight for one side
		double posW = allSamplesVec.stream()
				.filter((t)->t.positive && t.value <= cutPoint)
				.mapToDouble((t)->t.weight)
				.sum(); 
		double negW = allSamplesVec.stream()
				.filter((t)->(!t.positive) && t.value <= cutPoint)
				.mapToDouble((t)->t.weight)
				.sum(); 

		double entropyS1 = entropy(posW, negW);
		double W1 = posW + negW; 
		double entropyS2 = entropy(seenWeightPerLabel[1] - posW, 
				seenWeightPerLabel[0] - negW);
		double W2 = seenWeightPerLabel[2] - W1; 
		
		return priorEnt - (entropyS1 * W1 + entropyS2 * W2) / (W1 + W2);
	}
	
	protected static double XlogX(double x) {
	    return x * log2(x);
	}
	protected static double log2(double x) {
	    return Math.log(x) / Math.log(2);
	}
	
	@Override
	public void addSample(int label, Document doc, double weight) {
		// TODO Auto-generated method stub
		addSample(label,
				doc.getFeature(featureName) == null ? 0 : doc.getFeature(featureName).doubleValue(),
				weight);
	}

	@Override
	public double estimateRelevanceForLabel(int label){
		return 0;
	}
	
	@Override
	public int classify(Document doc) {
		double v = doc.getFeature(featureName) == null ? 0 : doc.getFeature(featureName).doubleValue();
		if(isHighRangePositive){
			return (long)(v * PRECISION_INV) > cutPoint ? 1 : 0;
		} else {
			return (long)(v * PRECISION_INV) <= cutPoint ? 1 : 0;
		}
	}

	protected class ValueT{
		boolean positive;
		long value;
		double weight;
		protected ValueT(boolean positive, long value, double weight){
			this.positive = positive;
			this.value = value;
			this.weight = weight;
		}
		
		public String toString(){
			return "<" + value + "," + positive + "," + weight + ">";
		}
	}

	@Override
	public double getPurityWhereExist() {
		throw new RuntimeException("Not implemented");
	}
}
