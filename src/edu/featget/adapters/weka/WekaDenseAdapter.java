package edu.featget.adapters.weka;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import edu.featgen.def.Document;
import edu.featgen.def.DocumentSet;
import edu.featgen.impl.Feature;
import edu.wiki.util.Tuple;

public class WekaDenseAdapter {
	public static final String TARGET_ATTR_NAME = "__target";
	private final DocumentSet docs;
	Attribute targetAttribute;
	String targetClass;
	Map<String,Attribute> featureMapping;
	
	public WekaDenseAdapter(DocumentSet docs, 
			String targetAttrName) {
		this(docs, targetAttrName, docs.collectFeatureNamesForClassification(targetAttrName));
	}

	public WekaDenseAdapter(DocumentSet docs, 
			String targetClass,
			Set<String>  featureNames) {
		this.docs = docs;
		this.targetClass = targetClass;
		featureMapping = new HashMap<>();
		featureNames.
			forEach((f)->featureMapping.put(f,new Attribute(f)));

		List<String> fvClassVal = new ArrayList<>(2);
		fvClassVal.add("negative");
		fvClassVal.add("positive");
		targetAttribute = new Attribute(TARGET_ATTR_NAME, fvClassVal);
	}
	
	public ArrayList<Attribute> getFeatureVector() {
		ArrayList<Attribute> featureVector = new ArrayList<Attribute>(featureMapping.size() + 1);
		featureVector.add(targetAttribute);
		featureMapping.forEach((name,feature)->{
			featureVector.add(feature);
		});
		return featureVector;
	}

	/**
	 * Currently supports mapping only Double typed features
	 * @return
	 */
	public Stream<Tuple<Instance,Document>> instanceStream() {
		return docs.stream()
				.map((doc)->{
					Instance instance = asInstance(doc);
					return new Tuple<>(instance,doc);
				});
	}
	
	public Instance asInstance(Document doc){
		int c = featureMapping.size();
		Instance instance = new DenseInstance(c + 1);
		
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
	}

}
