package edu.featgen.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import edu.featgen.def.FeatureSet;

public class FeatureSetImpl implements FeatureSet{

	private static final long serialVersionUID = 4987141340683573282L;

	String infoProviderGeneratorName;
	String name;
	boolean usedForClassification;
	Map<String, Object> features = new HashMap<>();
	
	public FeatureSetImpl(String name) {
		this.name = name;
	}
	
	@Override
	public String infoProviderGeneratorName(){
		return infoProviderGeneratorName;
	}
	@Override
	public void setInfoProviderGeneratorName(String infoProviderGeneratorName){
		this.infoProviderGeneratorName = infoProviderGeneratorName; 
	}
	
	@Override
	public Object get(String name) {
		return features.get(name);
	}

	@Override
	public Stream<Feature> stream() {
		return features.entrySet().stream().map((e)->new Feature(name + "_" + e.getKey(),e.getValue()));
	}

	@Override
	public Stream<Feature> streamRelativeNames() {
		return features.entrySet().stream().map((e)->new Feature(e.getKey(),e.getValue()));
	}

	@Override
	public void add(String name, Object value) {
		if(name.contains("_")){
			throw new RuntimeException("Feature name must not contain '_' char!");
		}
		features.put(name, value);
	}

	@Override
	public void remove(String name) {
		features.remove(name);
	}

	@Override
	public void removeAll() {
		features.clear();
	}

	@Override
	public void replaceAll(BiFunction<String, Object, Object> function) {
		features.replaceAll(function);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setUsedForClassification(boolean usedForClassification) {
		this.usedForClassification = usedForClassification;
	}

	@Override
	public boolean isUsedForClassification() {
		return usedForClassification;
	}
}