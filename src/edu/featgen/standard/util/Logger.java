package edu.featgen.standard.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class Logger {
	protected static String RESULTS_FILE = "results.txt";
	protected static String REPORT_FILE = "report.txt";

	public static void writeToConsole(String msg) {
		System.out.println(msg);
	}

	public static synchronized void writeToResults(String str) {
		try {
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(
			        new FileOutputStream(RESULTS_FILE, true)));
			pw.println(str);
			pw.close();
			System.out.println(str);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	public static synchronized void writeToReport(String str) {
		try {
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(
			        new FileOutputStream(REPORT_FILE, true)));
			pw.println(str);
			pw.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
