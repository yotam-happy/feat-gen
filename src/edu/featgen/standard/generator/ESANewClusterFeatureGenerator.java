package edu.featgen.standard.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.DoubleStream;

import org.joox.Match;

import edu.featgen.def.Document;
import edu.featgen.def.DocumentSet;
import edu.featgen.def.FeatureGenerator;
import edu.featgen.standard.util.ESAWrapper;
import edu.featgen.standard.util.InfoGainCalc;
import edu.wiki.index.agglomerative.AgglomerativeSampling;
import gnu.trove.TLongLongHashMap;

public class ESANewClusterFeatureGenerator implements FeatureGenerator{
	private static final long serialVersionUID = -2991255974066584945L;

	public static final String FEATURE_SET = "clustersNew";

	public static String COMBINER_MAX = "max";
	public static String COMBINER_SUM = "sum";

	ESAWrapper esa;
	
	private String className;
	private int nOfBestToUse; 
	private String sourceFeatureGenerator;
	private String sourceFeatureSet;
	public String name;
	private boolean usedForClassification;
	private int nClusters;
	
	boolean isSumCombiner;
	boolean isMaxCombiner;
	
	Map<Integer, Set<String>> clusters;
	Map<String,FeatureNode> inverseIndex;

	public ESANewClusterFeatureGenerator(String name,
			String sourceFeatureGenerator,
			String sourceFeatureSet,
			int nClusters,
			int nOfBestToUse,
			boolean usedForClassification,
			String combiner) {
		this.name = name;
		this.sourceFeatureGenerator = sourceFeatureGenerator;
		this.sourceFeatureSet = sourceFeatureSet;
		this.usedForClassification = usedForClassification;
		this.nClusters = nClusters;
		this.nOfBestToUse = nOfBestToUse;
		if(combiner.equals(COMBINER_MAX)){
			isMaxCombiner = true;
		}
		if(combiner.equals(COMBINER_SUM)){
			isSumCombiner = true;
		}
		
		esa = ESAWrapper.getInstance();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Set<String> getRequiredInputs() {
		return new HashSet<>(Arrays.asList(sourceFeatureGenerator));
	}


	class FeatureNode {
		String featureName;
		
		TLongLongHashMap values;
		double eval;
		
		FeatureNode(String featureName, TLongLongHashMap values){
			this.featureName = featureName;
			this.values = values;
		}
		FeatureNode(String featureName){
			this.featureName = featureName;
			this.values = new TLongLongHashMap();
		}
	}
	
	@Override
	public Object preProcess(DocumentSet training, Object transientData) {
		inverseIndex = new HashMap<>();
		double[] expectedWeightPerLabelHint = new double[2];
		System.out.println("Preparing clustering");

		// class counts
		training.forEach((docId, doc)->{
			int cls = doc.getClasses().contains(className) ? 1 : 0;
			expectedWeightPerLabelHint[cls] += 1;
		});

		Function<FeatureNode,Double> estimateInfoGain = (f)->{
			InfoGainCalc calc = new InfoGainCalc(2, false, f.featureName, InfoGainCalc.EstimationEnum.INFO_GAIN);
			f.values.forEachEntry((id,v)->{
				calc.addSample(training.document(id).getClasses().contains(className) ? 1 : 0, 
						v / InfoGainCalc.PRECISION_INV);
				return true;
			});
			calc.setExpectedWeightPerLabelHint(expectedWeightPerLabelHint);
			return calc.estimateRelevance();
		};

		Function<FeatureNode,Double> evaluator = (f)->{
			return f.eval;
		};

		BiFunction<FeatureNode,FeatureNode,FeatureNode> combiner = (f1,f2)->{
			FeatureNode combined = new FeatureNode(null);

			f1.values.forEachEntry((id,v)->{
				combined.values.put(id, v);
				return true;
			});
			f2.values.forEachEntry((id,v)->{
				long o = combined.values.get(id);
				if(isMaxCombiner){
					combined.values.put(id, v > o ? v : o);
				}else{ // if isSumCombiner
					combined.values.put(id, o + v);
				}
				return true;
			});

			combined.eval = estimateInfoGain.apply(combined);
			return combined;
		};
		
		// calculate feature-doc map
		Map<String,TLongLongHashMap> fmap = new HashMap<>();
		training.forEach((docId, doc)->{
			doc.getFeatureSet(sourceFeatureSet).forEach((f)->{
				TLongLongHashMap fmapt = fmap.get(f.getName());
				if(fmapt == null){
					fmapt = new TLongLongHashMap();
					fmap.put(f.getName(), fmapt);
				}
				fmapt.put(docId, (long)(((Double)f.doubleValue()) * InfoGainCalc.PRECISION_INV));
			});
		});
		
		ArrayList<FeatureNode> arrTemp = new ArrayList<>(); 
		// convert to featureNodes
		fmap.forEach((fname,arr)->{
			FeatureNode node = new FeatureNode(fname, arr);
			node.eval = estimateInfoGain.apply(node);
			arrTemp.add(node);
		});
		
		// optimization: get only best info gain features 
		Collections.sort(arrTemp, (n1,n2) -> -Double.compare(n1.eval, n2.eval));
		for(int i = 0; i < arrTemp.size() && i < nOfBestToUse; i++){
			inverseIndex.put(arrTemp.get(i).featureName, arrTemp.get(i));
			
		}
		
		System.out.println("Doing clustering");
				
		AgglomerativeSampling<FeatureNode> clustering = new AgglomerativeSampling<>
			(evaluator, combiner, inverseIndex.values());
		
		clustering.setMaxSamples(nOfBestToUse);
		clustering.doClustering(nClusters);
		
		// collect clusters
		clusters = new HashMap<>();
		clustering.forEachCluster((c) -> {
			Set<String> features = new HashSet<>();
			c.forEachLeaf((l)->features.add(l.getPoint().featureName));
			if(features.size() >= 1){
				clusters.put(clusters.size(), features);
			}
		});
		
		// release memory
		inverseIndex = null;
		return null;
	}

	@Override
	public void processDocument(Document doc, DocumentSet allDocs) {
		doc.addFeatureSet(FEATURE_SET, usedForClassification);
		clusters.forEach((i,c)->{
			DoubleStream stream = c.stream()
					.mapToDouble(
					(fn)->doc.getFeature(fn) == null ? 0 : doc.getFeature(fn).doubleValue());
			Double v = 0.0;
			if(isMaxCombiner){
				v = stream.max().orElse(0);
			}else{ // if isSumCombiner
				v = stream.sum();
			}
			
			if(v > 0){
				doc.getFeatureSet(FEATURE_SET).add(i.toString(), v);
			}
		});
	}
	
	@Override
	public void postProcess(DocumentSet docs) {
		// TODO Auto-generated method stub
		
	}
	
	public static FeatureGenerator instantiateFromXML(Match conf) {
		return new ESANewClusterFeatureGenerator(conf.attr("name"),
				conf.xpath("./SourceFeatureGenerator").content().trim(),
				conf.xpath("./SourceFeatureSet").content().trim(),
				Integer.parseInt(conf.xpath("./NumberOfClusters").content().trim()),
				Integer.parseInt(conf.xpath("./NumberOfBestToUse").content().trim()),
				Boolean.parseBoolean(conf.xpath("./UsedForClassification").content().trim()),
				conf.xpath("./SourceFeatureSet").content().trim());
	}

	@Override
	public void reset(DocumentSet docs, String className) {
		this.className = className;
		docs.forEach((id,doc)->doc.removeFeatureSet(FEATURE_SET));
	}
	@Override
	public boolean isRecalculateForEachClass() {
		return true;
	}

	@Override
	public String getFeatureDescription(String featureName){
		return null;
	}
	@Override
	public String getFeatureDescriptiveName(String featureName){
		return null;
	}
}
