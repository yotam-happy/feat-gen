package edu.featgen.def;

import java.io.Serializable;
import java.util.Set;

public interface FeatureSelector extends Serializable {
	String getName();
	Set<String> getFilteredFeatures(DocumentSet docs, String targetClass);
}
