package projectTest.classifier;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BEPCalculator {
	Set<PRScore> results = new HashSet<>();

	public BEPCalculator(){
	}
	
	public void addTestResult(double precision, double recall) {
		results.add(new PRScore(precision, recall));
	}
	
	public double getBEP() {
		List<PRScore> scores = results.stream().sorted((e1, e2)->Double.compare(e1.precision, e2.precision))
				.collect(Collectors.toList());
		scores.stream().forEach((s)->System.out.println("Precision: " + s.precision + " Recall: " + s.recall));
		return 0;
	}
	
	private class PRScore {
		double precision;
		double recall;
		PRScore(double precision, double recall) {
			this.precision = precision;
			this.recall = recall;
		}
	}
}
