package projectTest.classifier;

import java.util.Map;
import java.util.Set;

import edu.featgen.def.Document;
import edu.featgen.def.DocumentSet;

public interface ClassifierAdapter {
	public void buildClassifier(String topic, 
			DocumentSet docs, 
			Map<Long,Double> sampleWeight,
			Set<String> featuresUsed);
	public boolean classify(Document doc);

}
