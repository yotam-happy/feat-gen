package edu.featgen.standard.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.clustering.jot.algorithms.AlgorithmConstructor;
import edu.clustering.jot.datapoint.DenseEucledianPoint;
import edu.clustering.jot.interfaces.ClusteringAlgorithm;
import edu.clustering.jot.interfaces.Point;
import edu.clustering.jot.kmeans.KMeans;
import edu.featgen.def.Document;
import edu.featgen.def.DocumentSet;
import edu.featgen.def.FeatureGenerator;
import edu.featgen.standard.selector.InfoGainFeatureSelector;
import edu.featgen.standard.util.InfoGainCalc;
import edu.wiki.util.Tuple;

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
	protected Map<String,Double> featurePN;
	protected double d1,d2;
	protected int maxIterations = 25;
	protected double minDelta = 0.01;
			
	private String className;
	
	@SuppressWarnings("unchecked")
	@Override
	public Object preProcess(DocumentSet docs, Object transientData) {
		
		if(transientData != null){
			clusters = (Map<Integer, Cluster>) transientData; 
		} else if(!getClusterOnlyOnce() || clusters == null){
			// TODO: move this
			if (featurePN == null){
				Set<String> s = new HashSet<>(Arrays.asList(getSourceFeatureSet())); 
				List<Tuple<Double, String>> l = InfoGainFeatureSelector.getFeaturesRelevance(docs, 
						className, s, null, 
						()-> new InfoGainCalc(2, false, null, InfoGainCalc.EstimationEnum.POS));
				featurePN = new HashMap<>();
				l.forEach((t)->featurePN.put(t.y.substring(t.y.lastIndexOf('_') + 1), t.x));
			}
			List<Double> temp = new ArrayList<>();
			featurePN.forEach((n,d)->{
				if(d > 0.5) {
					temp.add(d);
				}
				});
			temp.sort((e1,e2)->e1.compareTo(e2));
			d1 = temp.get(temp.size() / 2);
			List<Double> temp2 = new ArrayList<>();
			featurePN.forEach((n,d)->{
				if(d < 0.5) {
					temp2.add(d);
				}
				});
			temp2.sort((e1,e2)->e1.compareTo(e2));
			d2 = temp2.get(temp2.size() / 2);
			
			
			
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
	
	public void clusteringStats(DocumentSet docs, String targetClass){
		Set<String> s = new HashSet<>(Arrays.asList(getSourceFeatureSet())); 
		List<Tuple<Double, String>> l = InfoGainFeatureSelector.getFeaturesRelevance(docs, 
				targetClass, s, null, 
				()-> new InfoGainCalc(2, false, null, InfoGainCalc.EstimationEnum.NEG));

		Map<Integer,List<Double>> v = new HashMap<>();
		l.forEach((t)->{
			String fname = t.y.substring(t.y.lastIndexOf('_')+1);
			 Map<Integer, Double> cl = featureCluster.get(fname);
			 if(cl == null){
				 return;
			 }
			 cl.forEach((clusterId,w)->{
				 List<Double> li = v.get(clusterId);
				 if(li == null){
					 li = new ArrayList<>();
					 v.put(clusterId, li);
				 }
				 li.add(t.x);
			 });
		});
		
		double[] mean = new double[1];
		double[] var = new double[1];
		v.forEach((id,li)->{
			
			double cmean = li.stream().filter((x)->x!=0.5)
					.mapToDouble((x)->x)
					.average().orElse(0);
			double cvar = li.stream().filter((x)->x!=0.5)
					.mapToDouble((x)->Math.pow(x-cmean,2))
					.average().orElse(0);
			mean[0] += cmean;
			var[0] += cvar;
		});
		mean[0] = mean[0] / v.size();
		var[0] = var[0] / v.size();
		System.out.println("clustering pos/neg mean " + mean[0] + " var: " + var[0]);
	}

	/**
	 * Helper function to work the kmeans++ algorithm
	 */

	protected Map<Integer,Cluster> KmeansPPHelper(int k, Map<String,Point> m){
		List<Point> l = Arrays.asList(m.values().toArray(new Point[0]));
		ClusteringAlgorithm<Point> clusterer = 
				AlgorithmConstructor.getKMeansPlusPlus(maxIterations, minDelta);

		//((KMeans<Point>)clusterer).setContraint(this::constraint);
		
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
	
	protected boolean constraint(Point p, edu.clustering.jot.kmeans.Cluster<Point> c){
		// we assume p is NamedPoint
		Double posNeg = featurePN.get(((NamedPoint)p).name);
		for (Point x : c.points){
			double posNeg2 = featurePN.get(((NamedPoint)x).name);
			if((posNeg < d2 && posNeg2 > d1) ||
					(posNeg > d1 && posNeg2 < d2)){
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void postProcess(DocumentSet docs) {
		// TODO Auto-generated method stub
		clusteringStats(docs, className);
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
	
	public class NamedPoint extends DenseEucledianPoint{
		private static final long serialVersionUID = 4340095254107167099L;

		protected String name;
		public NamedPoint(int d, String name){
			super(d);
			this.name = name;
		}
		public NamedPoint(int d, Metric m, String name){
			super(d,m);
			this.name = name;
		}
		public NamedPoint(double[] coords, Metric m, String name){
			super(coords,m);
			this.name = name;
		}
		
	}

}
