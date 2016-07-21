package edu.featgen.standard.replacer;

import java.util.HashMap;
import java.util.Map;

import edu.featgen.def.Document;
import edu.featgen.impl.Feature;
import edu.featgen.standard.util.ESAWrapper;
import edu.wiki.util.Tuple;

public class ESAFeatureReplacer {

	Map<String, Map<String, Integer>> cache = new HashMap<>();
	String esaFeatureSet;

	public ESAFeatureReplacer() {
	}

	public Feature getFeatureValue(Document doc, String featureName, Feature f) {
		if (f != null) {
			return f;
		}

		// find closest feature and replace
		featureName = convertFeatureName(featureName);
		Map<String, Integer> fm = getFeature(featureName);

		Tuple<Double, String> t = doc
				.getFeatureSet(esaFeatureSet)
				.stream()
				.map((candidate) -> {
					Map<String, Integer> m = getFeature(convertFeatureName(candidate
							.getName()));
					return new Tuple<Double, String>(similarity(fm, m),
							candidate.name);
				}).max((a, b) -> Double.compare(a.x, b.x)).orElse(null);

		Feature nf = new Feature(t.y, doc.getFeature(t.y));
		return nf;
	}

	protected String convertFeatureName(String fname) {
		return fname;
	}

	protected String getText(String fname) {
		return fname;
	}

	protected Map<String, Integer> getFeature(String key) {
		Map<String, Integer> f = cache.get(key);
		if (f == null) {
			f = ESAWrapper.getInstance().getBOW(getText(key), true);
			cache.put(key, f);
		}
		return f;
	}

	protected double similarity(Map<String, Integer> a, Map<String, Integer> b) {
		return 0;
	}
}
