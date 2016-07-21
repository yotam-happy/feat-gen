package edu.featgen.standard.util;

import edu.featgen.def.Document;

public interface WeakClassifierForAdaBoostFS {
	public void setExpectedWeightPerLabelHint(double[] expectedWeightPerLabelHint);
	public void addSample(int label, Document doc, double weight);
	public void buildClasifier();
	public int classify(Document doc);
}
