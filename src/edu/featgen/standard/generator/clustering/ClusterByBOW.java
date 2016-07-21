package edu.featgen.standard.generator.clustering;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.joox.Match;

import edu.clustering.jot.interfaces.Point;
import edu.featgen.def.DocumentSet;
import edu.featgen.def.FeatureGenerator;
import edu.featgen.standard.generator.AbstractClusterFeatureGennerator;
import edu.featgen.standard.generator.ESAFeatureGenerator;
import edu.wiki.concept.ArrayListConceptVector;
import edu.wiki.search.ESASearcher;
import edu.wiki.util.WikiprepESAdb;
import edu.wiki.util.db.InlinkQueryOptimizer;

/**
 * Clusters the FEATURES
 */
public class ClusterByBOW extends AbstractClusterFeatureGennerator{
	private static final long serialVersionUID = 2663627273657989441L;

	public static final String FEATURE_SET = "esa";
	public static final String TEMP_FILE = "concet_bow_vectors.dump";
	private int minInlinksToParticipate = 100;

	private String sourceFeatureGenerator;
	private String sourceFeatureSet;
	public String name;
	private boolean usedForClassification;
	private int k;
	Map<String,Integer> termIdMap = new HashMap<>();

	ESASearcher searcher = new ESASearcher();

	public ClusterByBOW(String name,
			String sourceFeatureGenerator,
			String sourceFeatureSet,
			boolean usedForClassification,
			int k) {
		this.name = name;
		this.sourceFeatureGenerator = sourceFeatureGenerator;
		this.sourceFeatureSet = sourceFeatureSet;
		this.usedForClassification = usedForClassification;
		this.k = k;
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

		Set<Integer> filter = getConceptFilterByDF(3, docs);
		Map<String, Point> vecs = getBOWVectors(filter);

		return KmeansPPHelper(k, vecs);
	}

	protected Set<Integer> getConceptFilterByDF(int minDF, DocumentSet docs){
		Map<Integer,Integer> conceptDF = new HashMap<>();
		
		docs.forEach((docId, doc)->{
			doc.getFeatureSet(FEATURE_SET).forEach((feature)->{
				int id = ESAFeatureGenerator.featureNameToId(feature.name);
				Integer c = conceptDF.get(id);
				conceptDF.put(id, c != null ? c + 1 : 1);
			});
		});
		
		Set<Integer> concepts = new HashSet<>();
		conceptDF.forEach((id,v)->{
			if (v >= minDF){
				concepts.add(id);
			}
		});
		
		return concepts;
	}
	
	@SuppressWarnings("unchecked")
	protected Map<String, Point> getBOWVectors(Set<Integer> conceptFilter){
		// This is essencially a one off task so we can save results somewhere
		if (new File(TEMP_FILE).exists()){
			try{
				FileInputStream fin = new FileInputStream(TEMP_FILE);
				@SuppressWarnings("resource")
				ObjectInputStream ois = new ObjectInputStream(fin);
				Map<Integer, Point> saved = (Map<Integer, Point>) ois.readObject();
				
				return saved.entrySet().stream()
						.filter(e -> conceptFilter.contains(e.getKey()))
						.collect(Collectors.toMap(p -> p.getKey().toString(), p -> p.getValue()));				
			}catch(IOException | ClassNotFoundException e){
				// Just do it again
			}
		}
		Map<Integer, Point> map = new HashMap<>();
		InlinkQueryOptimizer.getInstance().loadAll();
		WikiprepESAdb.getInstance().forEachResult("SELECT old_id, old_text FROM text", (rs)->{
			try {
				String text = new String(rs.getBytes(2), "UTF-8");
				int conceptId = rs.getInt(1);
				
	        	if (InlinkQueryOptimizer.getInstance().doQuery(conceptId) < 
	        			minInlinksToParticipate){
	        		return;
	        	}
				map.put(conceptId,
						getBOWVectorForConcept(text, conceptId));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			
		});
		
		try{
			FileOutputStream fout = new FileOutputStream(TEMP_FILE);
			@SuppressWarnings("resource")
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(map);
		}catch(IOException e){
			// Just do it again next time
		}

		return map.entrySet().stream()
				.filter(e -> conceptFilter.contains(e.getKey()))
				.collect(Collectors.toMap(p -> p.getKey().toString(), p -> p.getValue()));				
	}
	protected ArrayListConceptVector getBOWVectorForConcept(String text, int conceptId){
		HashMap<String,Integer> bow = searcher.getBOW(text, true);
		double nTerms = bow.values().stream().mapToDouble((x)->x).sum();

		HashMap<Integer,Double> v = new HashMap<>();
		bow.forEach((term,termCount)->{
			Integer termId = termIdMap.get(term);
			if(termId == null){
				termId = termIdMap.size();
				termIdMap.put(term, termId);
			}
			v.put(termId, (double)termCount / nTerms);
		});
		
		ArrayListConceptVector fastV = new ArrayListConceptVector(v.size());
		v.forEach((termId,tf)->fastV.add(termId,tf));
		fastV.setId(conceptId);
		fastV.setWeight(1);
		return fastV;
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
		return new ClusterByBOW(conf.attr("name"),
				conf.xpath("./SourceFeatureGenerator").content().trim(),
				conf.xpath("./SourceFeatureSet").content().trim(),
				Boolean.parseBoolean(conf.xpath("./UsedForClassification").content().trim()),
				Integer.parseInt(conf.xpath("./K").content().trim()));
	}

	@Override
	protected boolean getClusterOnlyOnce() {
		return true;
	}

}
