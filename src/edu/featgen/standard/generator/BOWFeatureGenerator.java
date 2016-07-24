package edu.featgen.standard.generator;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joox.Match;

import weka.core.stemmers.SnowballStemmer;
import weka.core.stemmers.Stemmer;
import weka.core.tokenizers.NGramTokenizer;
import edu.featgen.def.Document;
import edu.featgen.def.DocumentSet;
import edu.featgen.def.FeatureGenerator;
import edu.wiki.index.WikipediaAnalyzer;

public class BOWFeatureGenerator implements FeatureGenerator{
	private static final long serialVersionUID = -4600153347419089198L;

	public static String FEATURE_SET(int ngram) {
		return "bow" + (ngram == 1 ? "" : "_" + ngram + "gram"); 
	}
	
	private String sourceFeatureGenerator;
	private String sourceFeatureName;
	private String stopWordsFileName;
	private int maxNGram;
	private int countTreshhold;
	private boolean usedForClassification;
	public String name;
	
	Stemmer stemmer;
	
	Map<String, Double> ngram_counts;

	Set<String> stopWords = new HashSet<>();
	WikipediaAnalyzer analyzer;
	public BOWFeatureGenerator(String name,
			String sourceFeatureGenerator,
			String sourceFeatureName,
			Stemmer stemmer,
			int maxNGram,
			int countTreshhold,
			boolean usedForClassification,
			String stopWordsFileName) {
		this.name = name;
		this.sourceFeatureGenerator = sourceFeatureGenerator;
		this.sourceFeatureName = sourceFeatureName;
		this.stemmer = stemmer;
		this.maxNGram = maxNGram;
		this.usedForClassification = usedForClassification;
		this.countTreshhold = countTreshhold;
		this.stopWordsFileName =stopWordsFileName;
		analyzer = new WikipediaAnalyzer();
		getStopWords();
	}

	private void getStopWords(){
		String text = null;
		try {
			List<String> lines = Files.readAllLines(Paths.get(stopWordsFileName), Charset.defaultCharset());
			StringBuffer sb = new StringBuffer();
			for(String line : lines){
				sb.append(line).append(" ");
			}
			text = sb.toString();

			NGramTokenizer tokenizer = new NGramTokenizer(); 
			tokenizer.setNGramMinSize(1); 
			tokenizer.setNGramMaxSize(1); 
			tokenizer.setDelimiters("\\W");
			tokenizer.tokenize(text);

			while (tokenizer.hasMoreElements()) {
				String token = ((String)tokenizer.nextElement()).toLowerCase();
				stopWords.add(token);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public Set<String> getRequiredInputs() {
		return new HashSet<>(Arrays.asList(sourceFeatureGenerator));
	}

	private boolean tokenValidation(String token) {
		if(stopWords.contains(token)){
			return false;
		}
		if(token.matches(".*\\d+.*")){
			return false;
		}
		return true;
	}
	
	public void forEachNGram(Document doc, BiConsumer<String,Integer> f){
		String[] cyclicArr = new String[maxNGram-1];
		int cyclicArrPos = 0;

		featureSets().forEach((fs)->doc.addFeatureSet(fs, usedForClassification));
		
		String text = doc.getFeature(sourceFeatureName).stringValue();

		// Set the tokenizer 
		NGramTokenizer tokenizer = new NGramTokenizer(); 
		tokenizer.setNGramMinSize(1); 
		tokenizer.setNGramMaxSize(1); 
		tokenizer.setDelimiters("\\W");
		tokenizer.tokenize(text);

		while (tokenizer.hasMoreElements()) {
			String token = ((String)tokenizer.nextElement()).toLowerCase();
			if(!tokenValidation(token)){
				continue;
			}
			String stemmed = stemmer.stem(token);
			if (stemmed.isEmpty()) {
				continue;
			}
			
			f.accept(stemmed,1);

			// Add NGrams to BOW
			for (int i = 0; i < maxNGram; i++) {
				String ngram = stemmed;
				for (int j = 0; j < i - 1; j++) {
					ngram+= "_" + cyclicArr[(cyclicArrPos - j) % maxNGram];
					
					f.accept(ngram, j+1);
				}
			}

			// Add to cyclic array (for n-gram)
			if (maxNGram > 1) {
				cyclicArr[cyclicArrPos] = stemmed;
				cyclicArrPos = (cyclicArrPos++) % maxNGram;
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object preProcess(DocumentSet docs, Object transientData) {
		if (transientData != null){
			ngram_counts = (Map<String, Double>)transientData;
		} else {
			ngram_counts = new HashMap<String, Double>();
			docs.forEach((docId,doc)->{
				forEachNGram(doc,(ngram, i)->{
					ngram_counts.put(ngram, 
							1.0 + 
							(ngram_counts.get(ngram) == null ? 0 : ngram_counts.get(ngram)));
				});
			});
		}
		return ngram_counts;
	}

	@Override
	public void processDocument(Document doc) {
		forEachNGram(doc,(ngram, i)->{
			if (ngram_counts.get(ngram) != null && ngram_counts.get(ngram) >= countTreshhold){
				Double v = doc.getFeatureSet(FEATURE_SET(i)).getDouble(ngram);
				doc.getFeatureSet(FEATURE_SET(i)).add(ngram, new Double(1.0 + (v != null ? v : 0.0)));
			}
		});
	}

	public Set<String> featureSets() {
		return IntStream.range(1, maxNGram+1).mapToObj((i)->FEATURE_SET(i)).collect(Collectors.toSet());
	}
	
	public static FeatureGenerator instantiateFromXML(Match conf) {
		return new BOWFeatureGenerator(conf.attr("name"),
				conf.xpath("./SourceFeatureGenerator").content().trim(),
				conf.xpath("./SourceFeatureName").content().trim(),
				new SnowballStemmer(),
				Integer.parseInt(conf.xpath("./MaxNGram").content().trim()),
				Integer.parseInt(conf.xpath("./CountTreshhold").content().trim()),
				Boolean.parseBoolean(conf.xpath("./UsedForClassification").content().trim()),
				conf.xpath("./StopWordsFileName").content().trim());
	}

	@Override
	public void reset(DocumentSet docs, String className) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isRecalculateForEachClass() {
		return false;
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