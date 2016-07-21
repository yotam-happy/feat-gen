package edu.featgen.standard.generator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.clustering.jot.algorithms.AlgorithmConstructor;
import edu.clustering.jot.interfaces.ClusteringAlgorithm;
import edu.clustering.jot.interfaces.Point;
import edu.featgen.def.Document;
import edu.featgen.def.DocumentSet;
import edu.featgen.def.FeatureGenerator;

public abstract class AbstractClusterFeatureGennerator  implements FeatureGenerator{
	private static final long serialVersionUID = -948932903672189437L;

	public static final String FEATURE_SET = "clusters";

	// Expected to return a set of feature clusters
	
	protected abstract Map<Integer, Cluster> doClustering(String className, DocumentSet docs);
	protected abstract boolean isUsedForClassification();
	protected abstract String getSourceFeatureSet();
	protected abstract boolean getClusterOnlyOnce();
	
	Map<Integer, Cluster> clusters;
	Map<String, Map<Integer,Double>> featureCluster = new HashMap<>();
	protected int maxIterations = 25;
	protected double minDelta = 0.01;
			
	private String className;
	
	@SuppressWarnings("unchecked")
	@Override
	public Object preProcess(DocumentSet docs, Object transientData) {
		if(transientData != null){
			clusters = (Map<Integer, Cluster>) transientData; 
		}else{ //if(!getClusterOnlyOnce() || clusters == null){
			clusters = doClustering(getClusterOnlyOnce() ? null : className, docs);
		}
		
		// Calculate inverse map
		clusters.forEach((id,cluster)->{
			cluster.features.forEach((featureName,featureValue)->{
				if (!featureCluster.containsKey(featureName)){
					featureCluster.put(featureName, new HashMap<>());
				}
				featureCluster.get(featureName).put(id, featureValue);
			});
		});
		
		return clusters;
	}

	@Override
	public void processDocument(Document doc, DocumentSet allDocs) {
		doc.addFeatureSet(FEATURE_SET, isUsedForClassification());
		doc.getFeatureSet(getSourceFeatureSet()).streamRelativeNames().forEach((f)->{
			if (!featureCluster.containsKey(f.name)){
				return;
			}
			featureCluster.get(f.name).forEach((clusterId, factor)->{
				double d = doc.getFeatureSet(FEATURE_SET).get(clusterId.toString()) == null ? 
							0.0 : 
							doc.getFeatureSet(FEATURE_SET).getDouble(clusterId.toString());
				doc.getFeatureSet(FEATURE_SET).add(clusterId.toString(), d + f.doubleValue() * factor);
			});
		});
	}

	/**
	 * Helper function to work the kmeans++ algorithm
	 */

	protected Map<Integer,Cluster> KmeansPPHelper(int k, Map<String,Point> m){
		List<Point> l = Arrays.asList(m.values().toArray(new Point[0]));
		ClusteringAlgorithm<Point> clusterer = 
				AlgorithmConstructor.getKMeansPlusPlus(maxIterations, minDelta);
		clusterer.doClustering(k, k, l);
		List<Point> centroids = clusterer.getCentroids();
		
		Map<Integer,Cluster> clusters = new HashMap<>();
		// Create clusters
		for(int i = 0; i < centroids.size(); i++){
			clusters.put(i, new Cluster(i));
		}

		// populate clusters
		m.forEach((name, point)->{
			double bestD = Double.MAX_VALUE;
			int bestClusterId = -1;
			for(int i = 0; i < centroids.size(); i++){
				double D = point.distance(centroids.get(i));
				if (D < bestD){
					bestD = D;
					bestClusterId = i;
				}
			}
			if (bestClusterId != -1){
				clusters.get(bestClusterId).features.put(name, 1.0);
			}
		});

		return clusters;
	}
	
	@Override
	public void postProcess(DocumentSet docs) {
		// TODO Auto-generated method stub
		
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
	public String getFeatureDescription(String featureName) {
		return null;
	}

	@Override
	public String getFeatureDescriptiveName(String featureName) {
		return null;
	}

	public class Cluster{
		public int id;
		public Map<String,Double> features = new HashMap<>();
		
		public Cluster(int id){
			this.id = id;
		}
	}

}
