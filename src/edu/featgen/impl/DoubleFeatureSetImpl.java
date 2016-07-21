package edu.featgen.impl;

import java.util.function.BiFunction;
import java.util.stream.Stream;

import edu.featgen.def.FeatureSet;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

public class DoubleFeatureSetImpl  implements FeatureSet{
	private static final long serialVersionUID = -272991956272974472L;

	String infoProviderGeneratorName;
	String name;
	boolean usedForClassification;
	TObjectDoubleHashMap<String> features = new TObjectDoubleHashMap<>();
	
	public DoubleFeatureSetImpl(String name) {
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
		if(features.size() == 0){
			return Stream.empty();
		}
		TObjectDoubleIterator<String> iter = features.iterator();
		return Stream.generate(()->{
			iter.advance();
			Feature f = new Feature(name + "_" + iter.key(),iter.value());
			return f;
		}).limit(features.size());
	}

	@Override
	public Stream<Feature> streamRelativeNames() {
		if(features.size() == 0){
			return Stream.empty();
		}
		TObjectDoubleIterator<String> iter = features.iterator();
		return Stream.generate(()->{
			iter.advance();
			Feature f = new Feature(iter.key(),iter.value());
			return f;
		}).limit(features.size());
	}

	@Override
	public void add(String name, Object value) {
		if(name.contains("_")){
			throw new RuntimeException("Feature name must not contain '_' char!");
		}
		features.put(name, (Double)value);
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
		if(features.size() == 0){
			return;
		}
		TObjectDoubleIterator<String> iter = features.iterator();
		while(iter.hasNext()){
			iter.advance();
			Double d = (Double)function.apply(iter.key(), iter.value());
			iter.setValue(d);
		}
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
