package edu.featgen.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import edu.featgen.def.Document;
import edu.featgen.def.FeatureSet;

public class DocumentImpl implements Document{
	private static final long serialVersionUID = -3704635384625802589L;

	Map<String,FeatureSet> featureSets = new HashMap<>();
	boolean training = false;
	Set<String> classes = new HashSet<>();
	long id;

	public DocumentImpl(long id) {
		addFeatureSet(SOURCE_FEATURESET, false);
		this.id = id;
	}
	
	public FeatureSet getFeatureSet(String name) {
		return featureSets.get(name);
	}
	@Override

	public FeatureSet getSourceFeatureSet() {
		return featureSets.get(SOURCE_FEATURESET);
	}

	@Override
	public void addFeatureSet(String name, boolean usedForClassification) {
		if(name.contains("_")){
			throw new RuntimeException("Feature set name must not contain '_' char!");
		}
		FeatureSet fs = new FeatureSetImpl(name);
		fs.setUsedForClassification(usedForClassification);
		featureSets.put(name, fs);
	}

	@Override
	public void addDoubleFeatureSet(String name, boolean usedForClassification){
		if(name.contains("_")){
			throw new RuntimeException("Feature set name must not contain '_' char!");
		}
		FeatureSet fs = new DoubleFeatureSetImpl(name);
		fs.setUsedForClassification(usedForClassification);
		featureSets.put(name, fs);
	}

	@Override
	public Stream<FeatureSet> featureSetStream() {
		return featureSets.values().stream();
	}

	@Override
	public Feature getFeature(String name) {
		int c = name.indexOf('_');
		if (c < 0) {
			throw new RuntimeException("Feature name invalid");
		}
		String fsname = name.substring(0, c);
		String fname = name.substring(c + 1);
		FeatureSet fs = getFeatureSet(fsname);
		if (fs == null) {
			return null;
		}
		Object val = fs.get(fname);
		if (val == null) {
			return null;
		}
		return new Feature(name, val);
	}

	@Override
	public void removeFeature(String name) {
		int c = name.indexOf('_');
		if (c < 0) {
			throw new RuntimeException("Feature name invalid");
		}
		String fsname = name.substring(0, c);
		String fname = name.substring(c + 1);
		FeatureSet fs = getFeatureSet(fsname);
		if (fs == null) {
			return;
		}
		if (fs.get(fname) == null) {
			return;
		}
		fs.remove(fname);
	}

	@Override
	public boolean isTraining() {
		return training;
	}

	@Override
	public void setTraining(boolean training) {
		this.training = training;
	}

	@Override
	public Set<String> getClasses() {
		return classes;
	}

	@Override
	public void removeFeatureSet(String name) {
		featureSets.remove(name);
		
	}

	@Override
	public long getId() {
		return id;
	}
}
