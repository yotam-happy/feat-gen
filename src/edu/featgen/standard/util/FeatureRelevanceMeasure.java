package edu.featgen.standard.util;

/**
 * Used for fast measuring of feature relevance
 * @author yotamesh
 *
 */
public interface FeatureRelevanceMeasure {
	public void setExpectedWeightPerLabelHint(double[] expectedWeightPerLabelHint);
	public void addSample(int label, double value, double weight);
	public void addSample(int label, double value);
	public double estimateRelevance();
	public double estimateRelevanceForLabel(int label);
	public double getPurityWhereExist();

}
