package edu.featgen.standard.util;

import java.time.Duration;
import java.time.Instant;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ProgressReporter<T> {
	Instant start = null;
	Instant nextReport = null;
	long count = 0;
	Duration reportInterval;
	BiConsumer<Long, Duration> reportingF;
	public ProgressReporter(Duration reportInterval, BiConsumer<Long, Duration> reportingF){
		this.reportInterval = reportInterval;
		this.reportingF = reportingF;
	}
	
	public void countOne() {
		if (count==0) {
			start = Instant.now();
			nextReport = start.plus(reportInterval);
		}
		count++;
		if (Instant.now().isAfter(nextReport)) {
			nextReport = nextReport.plus(reportInterval);
			reportingF.accept(count, Duration.between(start, Instant.now()));
		}
	}
	
	// t is not used for anything, but allows this to be used inside a .map() pattern
	public Function<T, T> countFunc() {
		return (T t) -> {
			countOne();
			return t;
		};
	}
	public void finish() {
		reportingF.accept(count, Duration.between(start, Instant.now()));
	}
}
