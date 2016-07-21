package edu.featgen.standard.selector;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.joox.Match;

import edu.featgen.def.Document;
import edu.featgen.def.DocumentSet;
import edu.featgen.def.FeatureSelector;
import edu.featgen.standard.util.FeatureRelevanceMeasure;
import edu.featgen.standard.util.InfoGainCalc;
import edu.wiki.util.Tuple;
import gnu.trove.TLongDoubleHashMap;

public class AdaBoostFeatureSelector implements FeatureSelector {
	private static final long serialVersionUID = -1083567211431427957L;

	public static final int N_FEATURES_PER_ROUND = 10;
	Set<String> sourceFeatureSets;
	public String name;
	private int keepNFeatures;

	Set<String> selectedFeatures;
	Map<String, TLongDoubleHashMap> candidateFeatures;
	Set<String> allFeatures;
	boolean[] docPositive;
	
	public AdaBoostFeatureSelector(String name,
			Set<String> sourceFeatureSets,
			int keepNFeatures) {
		this.name = name;
		this.sourceFeatureSets = sourceFeatureSets;
		this.keepNFeatures = keepNFeatures;
	}

	@Override
	public String getName() {
		return name;
	}
	
	protected void initCandidateFeatures(List<Document> docsArr, String targetClass){
		for(int i = 0; i < docsArr.size(); i++){
			int j = i;
			docsArr.get(i).featureSetStream().filter((fs)->sourceFeatureSets.contains(fs.getName()))
				.flatMap((fs)->fs.stream())
				.forEach((f)->{
					if(candidateFeatures.get(f.getName()) == null){
						candidateFeatures.put(f.getName(), new TLongDoubleHashMap());
						allFeatures.add(f.getName());
					}
					candidateFeatures.get(f.getName()).put(j, f.doubleValue());
				});
		}
		// helps us get classification of missing values later
		candidateFeatures.forEach((f,m)->m.put(-1, 0));
	}
	
	public void adaBoostFeatureSelector(
			DocumentSet docs,
			List<Document> docsArr, 
			String targetClass){
/*		System.out.println("Starting AdaBoost Feature Selector");

		selectedFeatures = new HashSet<>();
		candidateFeatures = new HashMap<>();
		allFeatures = new HashSet<>();
		initCandidateFeatures(docsArr, targetClass);
		
		double[] D = new double[docsArr.size()];
		docPositive = new boolean[docsArr.size()];
		
		// init distribution
		for(int i = 0; i < D.length; i++){
			D[i] = 1.0 / D.length;
			docPositive[i] = docsArr.get(i).getClasses().contains(targetClass);
		}
		
		boolean nothing = false;
		while(selectedFeatures.size() < keepNFeatures){

			// Get next set of features
			Set<String> bestcandidates = getBestCandidateFeatures(
					D,docsArr, targetClass);
			
			if(bestcandidates.isEmpty()){
				for(int i = 0; i < D.length; i++){
					D[i] = 1.0 / D.length;
				}
				System.out.println("Got nothing...");
				if(nothing){
					System.out.println("Got nothing twice...finish");
					break;
				}
				nothing = true;
				continue;
			}
			nothing = false;
			
			bestcandidates.forEach((s)->{
				selectedFeatures.add(s);
				candidateFeatures.remove(s);
			});
			
			Map<Long,Double> Dmap = new HashMap<>();
			for(int i = 0; i < D.length; i++){
				Dmap.put(docsArr.get(i).getId(), D[i]);
			}
			// build weak classifier
			Experiment classifier = new SingleClassClassifier();
			classifier.train(targetClass, 
					docs, 
					false, 
					Dmap, 
					bestcandidates);
			
			// clculate loss
			double Loss = 0;
			for(int i = 0; i < D.length; i++){
				Evaluation eval = classifier.classify(docsArr.get(i));;
				if (!eval.actual == eval.predicted){
					Loss += D[i];
				}
			}
//			Loss /= D.length;
			
			double W = Math.log((1-Loss)/Loss) / 2; 
			
			// Calculate new D	
			double norm = 0;
			for(int i = 0; i < D.length; i++){
				Evaluation eval = classifier.classify(docsArr.get(i));;
				boolean correct = eval.actual == eval.predicted; 
				D[i] *= Math.exp(W * (correct ? -1 : 1));
				norm += D[i];
			}
			double diff =0;
			for(int i = 0; i < D.length; i++){
				D[i] /= norm;
				diff += Math.abs(D[i] - 1 / D.length);
			}
			
			
			
			System.out.println("AdaBoost Feature Selector done " + selectedFeatures.size() + " diff: " + diff);
		}*/
	}
	
	public static FeatureSelector instantiateFromXML(Match conf) {
		Set<String> sourceFeatureSets = conf.xpath("./SourceFeatureSet").contents()
				.stream().map((s)->s.trim()).collect(Collectors.toSet());
		return new AdaBoostFeatureSelector(conf.attr("name"),
				sourceFeatureSets,
				Integer.parseInt(conf.xpath("./KeepNFeatures").content().trim()));
	}

	@Override
	public Set<String> getFilteredFeatures(DocumentSet docs, String targetClass) {
		List<Document> docsArr = docs.stream().filter((d)->d.isTraining()).collect(Collectors.toList());
		adaBoostFeatureSelector(docs, docsArr,targetClass);
		allFeatures.removeAll(selectedFeatures);
		return allFeatures;
	}

	public Set<String> getBestCandidateFeatures(
			double D[], 
			List<Document> docsArr, 
			String targetClass){
		double[] expectedWeightPerLabel = new double[3];
		// class counts
		for(int i = 0; i < docsArr.size(); i++){
			int cls = docPositive[i] ? 1 : 0;
			expectedWeightPerLabel[cls] += D[i];
			expectedWeightPerLabel[2] += D[i];
		}

		double p = 1;
		Random rnd = new Random();
		List<Tuple<String,Double>> featureEval = candidateFeatures.entrySet().stream()
				.filter((e)->rnd.nextDouble() <= p) // sampling
				.map((e)->{
					FeatureRelevanceMeasure measure = new InfoGainCalc(2, e.getKey());
					
					measure.setExpectedWeightPerLabelHint(expectedWeightPerLabel);
					e.getValue().forEachEntry((id,v)->{
						if(id != -1 && D[(int)id] > 0){
							measure.addSample(docPositive[(int)id] ? 1 : 0, v, D[(int)id]);
						}
						return true;
					});
					
					return new Tuple<String,Double>(e.getKey(),measure.estimateRelevance());
				})
				.filter((t)->t.y > 0) // filter useless features
				.sorted((t1,t2)->-t1.y.compareTo(t2.y))
				.collect(Collectors.toList());
		
		Set<String> res = new HashSet<>();
		for(int i = 0; i < N_FEATURES_PER_ROUND && i < featureEval.size(); i++){
			res.add(featureEval.get(i).x);
		}
		return res;
	}
}
