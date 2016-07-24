package edu.featgen.standard.generator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.joox.Match;

import edu.featgen.def.Document;
import edu.featgen.def.DocumentSet;
import edu.featgen.def.FeatureGenerator;

/**
 * for scheme details:
 * http://nlp.stanford.edu/IR-book/html/htmledition/document-and-query-weighting-schemes-1.html
 *
 */
public class TfidfFeatureModifier implements FeatureGenerator{
	private static final long serialVersionUID = -4076777658433768713L;

	Map<String, Double> df;
	double numberTrainDocs;
	
	Set<String> sourceFeatureGenerators;
	Set<String> sourceFeatureSets;
	public String name;
	public String scheme;
	
	public TfidfFeatureModifier(String name,
			String scheme,
			Set<String> sourceFeatureGenerators, 
			Set<String> sourceFeatureSets) {
		this.name = name;
		this.scheme = scheme;
		this.sourceFeatureGenerators = sourceFeatureGenerators;
		this.sourceFeatureSets = sourceFeatureSets;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Set<String> getRequiredInputs() {
		return sourceFeatureGenerators;
	}

	@Override
	public Object preProcess(DocumentSet training, Object transientData) {
		df = new HashMap<>();
		// calculate df map
		training.forEach((docId, doc)->{
			sourceFeatureSets.forEach((fs) -> {
				doc.getFeatureSet(fs).streamRelativeNames().forEach((f)->{
					Double v = df.get(f.name);
					df.put(f.name, 1 + (v != null ? v : 0));
				});
			});
		});
		numberTrainDocs = training.size();
		return null;
	}

	@Override
	public void processDocument(Document doc) {
		double norm;
		if (scheme.charAt(2) == 'c'){
			norm = Math.sqrt(sourceFeatureSets.stream().map((fs)->doc.getFeatureSet(fs))
			.flatMap((fs)->fs.stream()).mapToDouble((f)->f.doubleValue() * f.doubleValue())
			.sum());
		} else {
			norm = 1.0;
		}

		Double aux;
		switch(scheme.charAt(0)){
		case 'a':
			aux = sourceFeatureSets.stream().map((fs)->doc.getFeatureSet(fs))
			.flatMap((fs)->fs.stream()).mapToDouble((f)->f.doubleValue())
			.max().orElse(0.0);			
			break;
		case 'L':
			aux = sourceFeatureSets.stream().map((fs)->doc.getFeatureSet(fs))
			.flatMap((fs)->fs.stream()).mapToDouble((f)->f.doubleValue())
			.average().orElse(0.0);
			break;
		default:
			aux = 0.0;
		}
		
		sourceFeatureSets.forEach((fs) -> {
			doc.getFeatureSet(fs).replaceAll((name,value)->{
				if(df.get(name) == null){
					// didn't appear in the training set, can't do nothing
					return value;
				}
				Double docFreq;
				switch(scheme.charAt(1)){
				case 'n': docFreq = 1.0; break;
				case 't': docFreq = Math.log(numberTrainDocs / df.get(name)); break;
				case 'p': docFreq = Math.max(0, Math.log((numberTrainDocs - df.get(name)) / df.get(name))); break;
				default: docFreq = 1.0;
				}

				Double tf = 0.0;
				switch(scheme.charAt(0)){
				case 'n': tf = (Double)value; break;
				case 'l': tf = 1 + Math.log((Double)value); break;
				case 'a': tf = 0.5 + 0.5 * (Double)value / aux; break;
				case 'b': tf = (Double)value > 0.1 ? 1.0 : 0.0; break;
				case 'L': tf = (1 + Math.log((Double)value)) / (1 + Math.log(aux)); break;
				}

				return tf * docFreq / norm;
			});
		});
	}

	public static FeatureGenerator instantiateFromXML(Match conf) {
		Set<String> sourceFeatureGenerators = conf.xpath("./SourceFeatureGenerator").contents()
				.stream().map((s)->s.trim()).collect(Collectors.toSet());
		Set<String> sourceFeatureSets = conf.xpath("./SourceFeatureSet").contents()
				.stream().map((s)->s.trim()).collect(Collectors.toSet());
		return new TfidfFeatureModifier(conf.attr("name"),
				conf.xpath("./Scheme").content().trim(),
				sourceFeatureGenerators, 
				sourceFeatureSets);
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
