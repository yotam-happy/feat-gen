package edu.featgen.standard.generator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.joox.Match;

import edu.featgen.def.Document;
import edu.featgen.def.DocumentSet;
import edu.featgen.def.FeatureGenerator;

public class RangeNormalizeFeatureModifier implements FeatureGenerator{
	private static final long serialVersionUID = 88139226161409843L;

	Set<String> sourceFeatureGenerators;
	Set<String> sourceFeatureSets;
	public String name;

	Map<String, Double> max = new HashMap<>();
	Map<String, Double> min = new HashMap<>();

	public RangeNormalizeFeatureModifier(String name,
			Set<String> sourceFeatureGenerators, 
			Set<String> sourceFeatureSets) {
		this.name = name;
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
		// calculate min, max map
		training.forEach((docId, doc)->{
			sourceFeatureSets.forEach((fs) -> {
				doc.getFeatureSet(fs).streamRelativeNames().forEach((f)->{
					Double oldMax = max.containsKey(f.name) ? max.get(f.name) : 0;
					Double oldMin = min.containsKey(f.name) ? min.get(f.name) : 0;
					max.put(f.name, Math.max(oldMax, f.doubleValue()));
					min.put(f.name, Math.min(oldMin, f.doubleValue()));
				});
			});
		});
		return null;
	}

	@Override
	public void processDocument(Document doc) {
		sourceFeatureSets.forEach((fs) -> {
			doc.getFeatureSet(fs).replaceAll((name,value)->{
				Double b = min.get(name);
				Double t = max.get(name);
				if (b == null || t == null){
					return 0.0;
				} else if (b == t){
					return 1.0;
				}
				return ((Double)value - b) / (t-b);
			});
		});
	}

	public static FeatureGenerator instantiateFromXML(Match conf) {
		Set<String> sourceFeatureGenerators = conf.xpath("./SourceFeatureGenerator").contents()
				.stream().map((s)->s.trim()).collect(Collectors.toSet());
		Set<String> sourceFeatureSets = conf.xpath("./SourceFeatureSet").contents()
				.stream().map((s)->s.trim()).collect(Collectors.toSet());
		return new RangeNormalizeFeatureModifier(conf.attr("name"),
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
