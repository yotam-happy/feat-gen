package edu.featgen.def;

import java.io.Serializable;
import java.util.Set;

public interface FeatureGenerator extends Serializable {
	String getName();

	/**
	 * Defines other feature generators that are required inputs
	 * to this one. This means the features they generate are the
	 * basis for the features this one generates.
	 * This also means this generator is only run after they are run
	 */
	Set<String> getRequiredInputs();

	Object preProcess(DocumentSet docs, Object transientData);
	void processDocument(Document doc, DocumentSet allDocs);
	void postProcess(DocumentSet docs);
	
	// reset is called for generators that are run for each class
	// with the class name. Should reset the generator state and
	// keep the className
	void reset(DocumentSet docs, String className);
	boolean isRecalculateForEachClass();
	
	String getFeatureDescription(String featureName);
	String getFeatureDescriptiveName(String featureName);
}