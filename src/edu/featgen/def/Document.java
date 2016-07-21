package edu.featgen.def;

import java.io.Serializable;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import edu.featgen.impl.Feature;

public interface Document extends Serializable {
	static final String SOURCE_FEATURESET = "source";

	long getId();
	FeatureSet getFeatureSet(String name);
	FeatureSet getSourceFeatureSet();
	void addFeatureSet(String name, boolean usedForClassification);
	void addDoubleFeatureSet(String name, boolean usedForClassification);
	void removeFeatureSet(String name);
	Feature getFeature(String name);
	Stream<FeatureSet> featureSetStream();
	void removeFeature(String name);

	boolean isTraining();
	void setTraining(boolean training);

	Set<String> getClasses();
	
	default Stream<Feature> featureStream(Predicate<? super Feature> predicate) {
		return featureSetStream()
				.flatMap((fs)->fs.stream())
				.filter(predicate);
	}

	default void forEachFeature(BiConsumer<? super String, ? super Object> action) {
		// docs streams
		featureStream((f)->true).forEach(f->action.accept(f.name, f.value));
	}
}
