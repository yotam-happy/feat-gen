package edu.featgen.standard.generator.clustering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joox.Match;

import edu.clustering.jot.datapoint.DenseEucledianPoint;
import edu.clustering.jot.interfaces.Point;
import edu.featgen.def.DocumentSet;
import edu.featgen.def.FeatureGenerator;
import edu.featgen.standard.generator.AbstractClusterFeatureGennerator;

public class ClusterByCorrelation extends AbstractClusterFeatureGennerator{
	private static final long serialVersionUID = 4912456916443612326L;

	private String sourceFeatureGenerator;
	private String sourceFeatureSet;
	public String name;
	private boolean usedForClassification;
	private List<Integer> kList;

	public ClusterByCorrelation(String name,
			String sourceFeatureGenerator,
			String sourceFeatureSet,
			boolean usedForClassification,
			List<Integer> kList) {
		this.name = name;
		this.sourceFeatureGenerator = sourceFeatureGenerator;
		this.sourceFeatureSet = sourceFeatureSet;
		this.usedForClassification = usedForClassification;
		this.kList = kList;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public Set<String> getRequiredInputs() {
		return new HashSet<>(Arrays.asList(sourceFeatureGenerator));
	}

	
	@Override
	protected Map<Integer, Cluster> doClustering(String category, DocumentSet docs) {
		System.out.println("Donig KMeans clustering");

		Set<String> filter = getConceptFilterByDF(3, docs);
		Map<String, Point> vecs = getVectors(filter, docs);

		Map<Integer, Cluster> clusters = new HashMap<>();
		for(Integer k : kList){
			KmeansPPHelper(k, vecs).forEach(
					(i,c)->clusters.put(clusters.size(), c));
		}
		return clusters;
	}

	protected Set<String> getConceptFilterByDF(int minDF, DocumentSet docs){
		Map<String,Integer> conceptDF = new HashMap<>();
		
		docs.forEach((docId, doc)->{
			doc.getFeatureSet(sourceFeatureSet).forEach((feature)->{
				Integer c = conceptDF.get(feature.name);
				conceptDF.put(feature.name, c != null ? c + 1 : 1);
			});
		});
		
		Set<String> concepts = new HashSet<>();
		conceptDF.forEach((concept,v)->{
			if (v >= minDF){
				concepts.add(concept);
			}
		});
		
		return concepts;
	}
	
	protected Map<String, Point> getVectors(Set<String> conceptFilter, DocumentSet docs){
		Map<String, DenseEucledianPoint> map = new HashMap<>();
		Map<String, Point> map2 = new HashMap<>();
		conceptFilter.forEach((concept)->{
			DenseEucledianPoint p = new DenseEucledianPoint(docs.size());
			map.put(concept, p);
			map2.put(concept, p);
		});

		docs.forEach((docId, doc)->{
			doc.getFeatureSet(sourceFeatureSet).forEach((f)->{
				if(map.containsKey(f.name)){
					map.get(f.name).set(docId.intValue(), f.doubleValue());
				}
			});
		});
		
		return map2;
	}

	@Override
	protected boolean isUsedForClassification() {
		return usedForClassification;
	}

	@Override
	protected String getSourceFeatureSet() {
		return sourceFeatureSet;
	}

	public static FeatureGenerator instantiateFromXML(Match conf) {
		return new ClusterByCorrelation(conf.attr("name"),
				conf.xpath("./SourceFeatureGenerator").content().trim(),
				conf.xpath("./SourceFeatureSet").content().trim(),
				Boolean.parseBoolean(conf.xpath("./UsedForClassification").content().trim()),
				parseK(conf.xpath("./K").content().trim()));
	}
	
	public static List<Integer> parseK(String kStr){
		List<Integer> ret = new ArrayList<>();
		for(String k : kStr.split(",")){
			ret.add(Integer.parseInt(k.trim()));
		}
		return ret;
	}

	@Override
	protected boolean getClusterOnlyOnce() {
		return true;
	}

}
