package edu.featgen;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joox.Match;

import projectTest.classifier.ClassifierAdapter;
import projectTest.classifier.Evaluation;
import projectTest.classifier.Evaluations;
import edu.featgen.def.DocumentSet;
import edu.featgen.standard.selector.InfoGainFeatureSelector;
import edu.featget.adapters.libsvm.LibSVMClassifier;
import edu.wiki.util.Tuple;

public class Experiment {
	Documents docs;
	ClassifierAdapter classifier;
	
	boolean repeatedExperiment;
	int bestOf;
	double evaluationSetSize;
	String classifierName;
	String modelFileName;
	boolean useSavedModel;
	boolean saveModel;
	
	public Experiment(boolean repeatedExperiment,
			int bestOf,
			double evaluationSetSize,
			String classifier,
			boolean useSavedModel,
			boolean saveModel,
			String modelFile){
		this.repeatedExperiment = repeatedExperiment;
		this.bestOf = bestOf;
		this.evaluationSetSize = evaluationSetSize;
		this.classifierName = classifier;
		this.useSavedModel = useSavedModel;
		this.saveModel = saveModel;
		this.modelFileName = modelFile;
	}

	public void setDocuments(Documents docs){
		this.docs = docs;
	}

	public Documents getDocuments(){
		return docs;
	}
	
	public void processDocuments(){
		// Either load processed document, or process them and possibly save result
		if(useSavedModel){
			try {
				FileInputStream fout = new FileInputStream(modelFileName);
				ObjectInputStream ois = new ObjectInputStream(fout);
				docs.deserialize(ois);		
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			docs.process(new HashMap<>());
			if(saveModel){
				try {
					FileOutputStream fout = new FileOutputStream(modelFileName);
					ObjectOutputStream oos = new ObjectOutputStream(fout);
					docs.serialize(oos);		
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		
	}

	public Tuple<Evaluations,Evaluations> testForTopic(String topic){
		if(repeatedExperiment){
			List<Tuple<DocumentSet,DocumentSet>> split = 
					docs.getCrossValidationSplit(5);
			
			double bestF = 0;
			Map<String,Object> bestTransientData = null;
			for(int i = 0; i < bestOf; i++){

				Map<String,Object> transientData = new HashMap<>();
				Evaluations evals = new Evaluations();
				for(Tuple<DocumentSet,DocumentSet> train_eval : split){
					train(topic, train_eval.x, null, null, transientData);
					evaluate(train_eval.y, topic, evals, transientData);
				}
				
				System.out.println("Repeated experiment attemp " + i + " got fMeasure=" + evals.fMeasure());
				if (evals.fMeasure() > bestF){
					bestF = evals.fMeasure();
					bestTransientData = transientData;
				}
			}
			
			System.out.println("Estimated fMeasure by cross validation = " + bestF);
			DocumentSet trainAll = docs.getTrainingSet();
			DocumentSet test = docs.getTestingSet();
			train(topic, trainAll, null, null, bestTransientData);
			Evaluations evaluations = evaluate(test, topic, bestTransientData);
			Evaluations evaluationsTrain = evaluate(trainAll, topic, bestTransientData);
			System.out.println("Actual fMeasure " + evaluations.fMeasure());
			return new Tuple<Evaluations,Evaluations>(evaluations,evaluationsTrain);
		} else {
			DocumentSet train = docs.getTrainingSet();
			DocumentSet test = docs.getTestingSet();

			train(topic, train, null, null, new HashMap<>());
			Evaluations evaluations = evaluate(test, topic, new HashMap<>());
			Evaluations evaluationsTrain = evaluate(train, topic, new HashMap<>());
			return new Tuple<Evaluations,Evaluations>(evaluations,evaluationsTrain);
		}
	}

	public Evaluations evaluate(
			DocumentSet docs, 
			String topic,
			Map<String, Object> transientData){
		return evaluate(docs, topic, new Evaluations(), transientData);
	}
	public Evaluations evaluate(
			DocumentSet docs, 
			String topic, 
			Evaluations evaluations,
			Map<String, Object> transientData){
		// do topic specific processing
		docs.process(topic, transientData);
		docs.forEach((id,doc)->{
				boolean actual = doc.getClasses().contains(topic);
				boolean predicted = classifier.classify(doc);
				evaluations.add(new Evaluation(doc, predicted, actual));
			});
		return evaluations;
	}
	
	public void train(String topic, 
			DocumentSet docs, 
			Map<Long,Double> sampleWeight,
			Set<String> featuresToUse,
			Map<String, Object> transientData){
		// do topic specific processing
		docs.process(topic, transientData);

		Set<String> featuresUsed = featuresToUse == null ? 
				docs.collectFeatureNamesForClassification(topic) :
				featuresToUse;
				
		InfoGainFeatureSelector.reportForFeatures(featuresUsed, docs, topic);
		
		classifier = new LibSVMClassifier();
		classifier.buildClassifier(topic, docs, sampleWeight, featuresUsed);
	}
	
	
	public static Experiment instantiateFromXML(Match conf) {
		return new Experiment(
				Boolean.parseBoolean(conf.xpath("./RepeatedExperiment").content().trim()),
				Integer.parseInt(conf.xpath("./BestOf").content().trim()),
				Double.parseDouble(conf.xpath("./EvaluationSetSize").content().trim()),
				conf.xpath("./Classifier").content().trim(),
				Boolean.parseBoolean(conf.xpath("./UseSavedModel").content().trim()),
				Boolean.parseBoolean(conf.xpath("./SaveModel").content().trim()),
				conf.xpath("./ModelFile").content().trim());
	}
}
