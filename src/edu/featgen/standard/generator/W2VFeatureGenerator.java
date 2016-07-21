package edu.featgen.standard.generator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.joox.Match;

import edu.featgen.def.Document;
import edu.featgen.def.DocumentSet;
import edu.featgen.def.FeatureGenerator;
import edu.featgen.standard.util.ESAWrapper;
import edu.featgen.standard.util.Word2VecReader;

public class W2VFeatureGenerator implements FeatureGenerator{
	private static final long serialVersionUID = -2037283396625651483L;

	public static final String FEATURE_SET = "w2v";

	private String sourceFeatureGenerator;
	private String sourceFeatureName;
	public String name;
	public String embeddingFileName;
	private boolean usedForClassification;

	int embeddingSize;
	Map<String,double[]> embeddings;
	
	public W2VFeatureGenerator(String name,
			String sourceFeatureGenerator,
			String sourceFeatureName,
			String embeddingFileName,
			boolean usedForClassification) {
		this.name = name;
		this.sourceFeatureGenerator = sourceFeatureGenerator;
		this.sourceFeatureName = sourceFeatureName;
		this.usedForClassification = usedForClassification;
		this.embeddingFileName = embeddingFileName;
		readEmbeddings();
	}
	
	protected void readEmbeddings(){
		embeddings = new HashMap<>();
		new Word2VecReader(this.embeddingFileName).forEach((t,e)->embeddings.put(t, e), null);
	}
	
	protected void normalizeStandardDev(){
		double[] mean = new double[embeddingSize];
		embeddings.values().forEach((e)->{
			for(int i = 0; i < embeddingSize; i++){
				mean[i] += e[i];
			}
		});
		for(int i = 0; i < embeddingSize; i++){
			mean[i] /= embeddings.size();
		}
		
		double[] dev = new double[embeddingSize];
		embeddings.values().forEach((e)->{
			for(int i = 0; i < embeddingSize; i++){
				dev[i] += Math.pow(e[i] - mean[i], 2);
			}
		});
		for(int i = 0; i < embeddingSize; i++){
			dev[i] = Math.sqrt(dev[i]);
		}
		
		embeddings.values().forEach((e)->{
			for(int i = 0; i < embeddingSize; i++){
				e[i] /= dev[i];
			}
		});
		
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
	public Object preProcess(DocumentSet docs, Object transientData) {
		return null;
	}
	
	@Override
	public void processDocument(Document doc, DocumentSet allDocs) {
		doc.addFeatureSet(FEATURE_SET, usedForClassification);

		String text = doc.getFeature(sourceFeatureName).stringValue();
		Map<String,Integer> bow = ESAWrapper.getInstance().getBOW(text, false);

		int norm = bow.values().stream().mapToInt((i)->i).sum();
		if(norm <= 0){
			return;
		}
		
		double[] embedding = new double[embeddingSize];
		bow.forEach((w,c)->{
			double[] e = embeddings.get(w);
			if(e != null){
				for(int i = 0; i < embeddingSize; i++){
					embedding[i] += e[i] * c;
				}
			}
		});
		
		// average
		for(int i = 0; i < embeddingSize; i++){
			embedding[i] /= (double)norm;
		}

		// add features
		for(int i = 0; i < embeddingSize; i++){
			doc.getFeatureSet(FEATURE_SET).add(Integer.toString(i), new Double(embedding[i]));
		}
	}
	
	@Override
	public void postProcess(DocumentSet docs) {
	}

	@Override
	public void reset(DocumentSet docs, String className) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isRecalculateForEachClass() {
		// TODO Auto-generated method stub
		return false;
	}

	public static FeatureGenerator instantiateFromXML(Match conf) {
		return new W2VFeatureGenerator(conf.attr("name"),
				conf.xpath("./SourceFeatureGenerator").content().trim(),
				conf.xpath("./SourceFeatureName").content().trim(),
				conf.xpath("./EmbeddingFile").content().trim(),
				Boolean.parseBoolean(conf.xpath("./UsedForClassification").content().trim()));
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
