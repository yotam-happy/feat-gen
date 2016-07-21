package edu.featget.adapters.weka;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.SparseInstance;
import edu.featgen.def.Document;
import edu.featgen.def.DocumentSet;
import edu.featgen.impl.Feature;

public class WekaSparseAdapter {
	public static final String TARGET_ATTR_NAME = "__target";
	private final DocumentSet docs;
	Attribute targetAttribute;
	String targetClass;
	Map<String,Attribute> featureMapping;
	
	public WekaSparseAdapter(DocumentSet docs, 
			String targetClass) {
		this(docs, targetClass, docs.collectFeatureNamesForClassification(targetClass));
	}

	public WekaSparseAdapter(DocumentSet docs, 
			String targetClass,
			Set<String>  featureNames) {
		this.docs = docs;
		this.targetClass = targetClass;
		featureMapping = new HashMap<>();
		featureNames.
			forEach((f)->featureMapping.put(f,new Attribute(f)));
	}
	
	public ArrayList<Attribute> getFeatureVector() {
		ArrayList<Attribute> featureVector = new ArrayList<Attribute>(featureMapping.size() + 1);

		List<String> fvClassVal = new ArrayList<>(2);
		fvClassVal.add("negative");
		fvClassVal.add("positive");
		targetAttribute = new Attribute(TARGET_ATTR_NAME, fvClassVal);
		featureVector.add(targetAttribute);

		featureMapping.forEach((name,feature)->featureVector.add(feature));
		return featureVector;
	}

	public Stream<Instance> instanceStream() {
		return instanceStream(null);
	}
	
	/**
	 * Currently supports mapping only Double typed features
	 * @return
	 */
	public Stream<Instance> instanceStream(Predicate<? super Document> documentPredicate) {
		return docs.stream()
			.filter(documentPredicate)
			.map((doc)->{
				int c = (int)doc.featureStream((f)->featureMapping.containsKey(f.name)).count();
				Instance instance = new SparseInstance(c + 1);
				
				if (doc.getClasses().contains(targetClass)) {
					instance.setValue(targetAttribute, "positive");
				} else {
					instance.setValue(targetAttribute, "negative");
				}
				
				featureMapping.forEach((name,attr)->{
					Feature f = doc.getFeature(name);
					instance.setValue(attr, f == null ? 0.0 : f.doubleValue());
				});
				return instance;
			});
	}
}
