package projectTest.classifier;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class Evaluations {
	Set<Evaluation> evaluations = new HashSet<>();
	
	public void add(Evaluation eval){
		evaluations.add(eval);
	}
	public int size(){
		return evaluations.size();
	}
	public void forEach(Consumer<Evaluation> consumer){
		evaluations.forEach(consumer);
	}
	public double countTP(){
		return evaluations.stream().mapToDouble((e)->e.actual && e.predicted ? 1.0 : 0.0).sum();
	}
	public double countFP(){
		return evaluations.stream().mapToDouble((e)->!e.actual && e.predicted ? 1.0 : 0.0).sum();
	}
	public double countTN(){
		return evaluations.stream().mapToDouble((e)->!e.actual && !e.predicted ? 1.0 : 0.0).sum();
	}
	public double countFN(){
		return evaluations.stream().mapToDouble((e)->e.actual && !e.predicted ? 1.0 : 0.0).sum();
	}
	
	public double precision(){
		double tp = countTP();
		double fp = countFP();
		return tp + fp == 0 ? 1 : tp / (tp + fp);
	}
	public double recall(){
		double tp = countTP();
		double fn = countFN();
		return tp + fn == 0 ? 1 : tp / (tp + fn);
	}
	public double fMeasure(){
		return (2 * precision() * recall()) / (precision() + recall());
	}
	public double rate(){
		return (countTP()+countTN())/evaluations.size();
	}
}
