package edu.featgen.def;

import java.io.Serializable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

import edu.featgen.impl.Feature;

/**
 * Using Object as feature values is ugly as hell... but i don't know
 * how to implement this otherwise as i cannot tell which type of feature
 * sets will a document have...
 * 
 * @author yotamesh
 *
 */
public interface FeatureSet extends Serializable{
	void setUsedForClassification(boolean usedForClassification);
	boolean isUsedForClassification();
	String getName();
	void add(String name, Object value);
	Object get(String name);
	void remove(String name);
	void removeAll();
	Stream<Feature> streamRelativeNames();
	Stream<Feature> stream();
	
	String infoProviderGeneratorName();
	void setInfoProviderGeneratorName(String name);
	
	void replaceAll(BiFunction<String,Object,Object> function);

	default void forEach(Consumer<Feature> action) {
		stream().forEach(action);
	}
	
	default String getString(String name) {
		return (String)get(name);
	}
	
	default Double getDouble(String name) {
		return (Double)get(name);
	}

	default Double getInt(String name) {
		return (Double)get(name);
	}
}
