package edu.featget.adapters.weka;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import projectTest.classifier.ClassifierAdapter;
import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import edu.featgen.def.Document;
import edu.featgen.def.DocumentSet;

public class WekaClassifier implements ClassifierAdapter{
	protected Classifier classifier;
	WekaDenseAdapter adapter;
	ArrayList<Attribute> featureVector;
	Instances training;

	public void buildClassifier(String topic, 
			DocumentSet docs, 
			Map<Long,Double> sampleWeight,
			Set<String> featuresUsed){
		adapter = new WekaDenseAdapter(docs, 
				topic, 
				featuresUsed);

		// get instances
		featureVector = adapter.getFeatureVector();

		training = new Instances("allInstances", featureVector, (int)docs.size());
		adapter.instanceStream()
				.forEach((t)->{
					if(sampleWeight != null){
						t.x.setWeight(sampleWeight.get(t.y.getId()));
					}
					training.add(t.x);
				});
		
		classifier = new SMO();

		System.out.println("Building model");
		Attribute toTest = training.attribute(WekaSparseAdapter.TARGET_ATTR_NAME);
		training.setClass(toTest);
		
		try {
			classifier.buildClassifier(training);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public Classifier getClassifier() {
		return classifier;
	}

	public boolean classify(Document doc) {
		Instances instances = new Instances("allInstances", featureVector, 1);
		Attribute toTest = instances.attribute(WekaSparseAdapter.TARGET_ATTR_NAME);
		instances.setClass(toTest);

		Instance inst = adapter.asInstance(doc);
		inst.setDataset(instances);
		try {
			double d = getClassifier().classifyInstance(inst);
			return d > 0.5;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
}
