package edu.featget.adapters.libsvm;

import java.util.Map;
import java.util.Set;

import projectTest.classifier.ClassifierAdapter;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import edu.featgen.def.Document;
import edu.featgen.def.DocumentSet;
import edu.wiki.util.Tuple;

/**
 * Tests SVM meta parameters (C,gamma) using a grid search.
 * Note that this uses the entire training set for training and
 * test set for testing so it does not represent automatic 
 * 'parameter tuning' in it's pure sense (that uses a subset of the
 * training as a validation set) 
 */
public class LibSVMClassifier implements ClassifierAdapter{
	
	svm_model model;
	LibSVMAdapter libSVMAdapter;
	
	public static svm_parameter getBasicSvmParameters(double C, double gamma){
		svm_parameter param = new svm_parameter();
		// default values
		param.svm_type = svm_parameter.C_SVC;
		param.kernel_type = svm_parameter.LINEAR;
		param.degree = 3;
		param.gamma = gamma;
		param.coef0 = 0;
		param.nu = 0.5;
		param.cache_size = 100;
		param.C = C;
		param.eps = 1e-3;
		param.p = 0.1;
		param.shrinking = 0;
		param.probability = 0;
		param.nr_weight = 0;
		param.weight_label = new int[0];
		param.weight = new double[0];
		return param;
	}

	@Override
	public void buildClassifier(String topic, DocumentSet docs,
			Map<Long, Double> sampleWeight, Set<String> featuresUsed) {
		libSVMAdapter = new LibSVMAdapter(docs, topic, featuresUsed);
		svm_problem prob = libSVMAdapter.getSvmProblem();

		svm_parameter p = getBasicSvmParameters(0.1, 1 / featuresUsed.size());
		String error_msg = svm.svm_check_parameter(prob,p);
		if (error_msg != null){
			System.out.println("LibSVM parameters invalid:");
			System.out.println(error_msg);
			throw new RuntimeException("LibSVM parameters invalid");
		}
		
		svm.svm_set_print_string_function(new libsvm.svm_print_interface(){
		    @Override public void print(String s) {} // Disables svm output
		});
		try {
			model = svm.svm_train(prob,p);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean classify(Document doc) {
		Tuple<svm_node[],Double> t = libSVMAdapter.getInstance(doc);
		double predicted = svm.svm_predict(model,t.x);
		return predicted > 0 ? true : false;
	}

}
