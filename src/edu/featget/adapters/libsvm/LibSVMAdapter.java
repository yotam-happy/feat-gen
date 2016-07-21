package edu.featget.adapters.libsvm;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import libsvm.svm_node;
import libsvm.svm_problem;
import weka.core.Attribute;
import edu.featgen.def.Document;
import edu.featgen.def.DocumentSet;
import edu.wiki.util.Tuple;

public class LibSVMAdapter {
	public static final String TARGET_ATTR_NAME = "__target";
	private final DocumentSet docs;
	Attribute targetAttribute;
	String targetAttrName;
	String[] features;
	
	public LibSVMAdapter(DocumentSet docs, 
			String targetAttrName,
			Set<String> featuresUsed) {
		this.docs = docs;
		this.targetAttrName = targetAttrName;
		
		features = new String[featuresUsed.size()];
		int i = 0;
		for(String f : featuresUsed){
			features[i] = f;
			i++;
		}
	}
	
	public svm_problem getSvmProblem(){
		svm_problem prob = new svm_problem();
		List<Document> list = docs.stream().collect(Collectors.toList());
		prob.l = list.size();
		prob.x = new svm_node[prob.l][];
		prob.y = new double[prob.l];

		for(int i = 0; i < list.size(); i++){
			Tuple<svm_node[],Double> t = getInstance(list.get(i));
			prob.x[i] = t.x;
			prob.y[i] = t.y;
		}
		return prob;
	}
	
	public double getY(Document doc){
		return doc.getClasses().contains(targetAttrName) ? 1.0 : -1.0;
	}

	public Tuple<svm_node[],Double> getInstance(Document doc){
		int s = 0;
		for(int j = 0; j < features.length; j++){
			if(doc.getFeature(features[j]) != null){
				s++;
			}
		}
		
		svm_node[] x = new svm_node[s];
		int k = 0;
		for(int j = 0; j < features.length; j++){
			if(doc.getFeature(features[j]) != null){
				x[k] = new svm_node();
				x[k].index = j;
				x[k].value = doc.getFeature(features[j]) == null ? 0.0 : doc.getFeature(features[j]).doubleValue();
				k++;
			}
		}
		
		return new Tuple<>(x, getY(doc));
	}
}
