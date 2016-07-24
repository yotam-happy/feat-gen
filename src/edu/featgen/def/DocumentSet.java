package edu.featgen.def;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import edu.wiki.util.Tuple;

public interface DocumentSet {
	Stream<Document> stream();
	Document document(long id);
	int size();
	public Set<String> collectFeatureNamesForClassification(String targetAttrName);
	public Set<String> collectFeatureNamesForFeatureSets(Set<String> fsName);
	public Set<String> collectFeatureNames(
			Predicate<? super FeatureSet> fsPredicate, 
			Predicate<? super String> featurePredicate);
	
	public void forEach(BiConsumer<Long,Document> action);

	public DocumentSet getTrainingSet();
	public DocumentSet getTestingSet();
	public Tuple<DocumentSet,DocumentSet> getTrainValidationSplit(double validationFrac);
	public List<Tuple<DocumentSet,DocumentSet>> getCrossValidationSplit(int folds);
	
	public String getFeatureDescription(String fname);
	public String getFeatureDescriptiveName(String fname);
	
	default void removeFeature(String name) {
		stream().forEach((doc)->doc.removeFeature(name));
	}

	public void readDataSet();
	public void processForTrain(String forClass, Map<String,Object> transientData);
	public void processForTest(String forClass);

	public void serialize(ObjectOutputStream oos);
	public void deserialize(ObjectInputStream ois);
}

