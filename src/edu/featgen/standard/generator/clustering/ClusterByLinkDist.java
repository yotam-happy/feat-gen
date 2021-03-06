package edu.featgen.standard.generator.clustering;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joox.Match;

import edu.clustering.jot.interfaces.Point;
import edu.featgen.def.DocumentSet;
import edu.featgen.def.FeatureGenerator;
import edu.featgen.standard.generator.AbstractClusterFeatureGennerator;
import edu.featgen.standard.generator.ESAFeatureGenerator;

public class ClusterByLinkDist  extends AbstractClusterFeatureGennerator{
	private static final long serialVersionUID = -7611261403038287238L;

	public static final String TEMP_FILE = "concet_link_vectors.dump";
	private int minInlinksToParticipate = 100;

	private String sourceFeatureGenerator;
	private String sourceFeatureSet;
	public String name;
	private boolean usedForClassification;
	private List<Integer> kList;

	public ClusterByLinkDist(String name,
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

		Set<Integer> filter = getConceptFilterByDF(3, docs);
		Map<String, Point> vecs = getLinkDistVectors(filter);

		Map<Integer, Cluster> clusters = new HashMap<>();
		for(Integer k : kList){
			KmeansPPHelper(k, vecs).forEach(
					(i,c)->clusters.put(clusters.size(), c));
		}
		return clusters;
	}

	protected Set<Integer> getConceptFilterByDF(int minDF, DocumentSet docs){
		Map<Integer,Integer> conceptDF = new HashMap<>();
		
		docs.forEach((docId, doc)->{
			doc.getFeatureSet(sourceFeatureSet).forEach((feature)->{
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
	protected Map<String, Point> getLinkDistVectors(Set<Integer> conceptFilter){
		// This is essentially a one off task so we can save results somewhere
		if (new File(TEMP_FILE).exists()){
			try{
				FileInputStream fin = new FileInputStream(TEMP_FILE);
				@SuppressWarnings("resource")
				ObjectInputStream ois = new ObjectInputStream(fin);
				return (Map<String, Point>) ois.readObject();
			}catch(IOException | ClassNotFoundException e){
				// Just do it again
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		}

		Map<String, Point> map = new HashMap<>();

		
		
		
		try{
			FileOutputStream fout = new FileOutputStream(TEMP_FILE);
			@SuppressWarnings("resource")
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(map);
		}catch(IOException e){
			// Just do it again next time	
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		
		return map;
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
		return new ClusterByLinkDist(conf.attr("name"),
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
