package edu.featgen.standard.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import jgibblda.Estimator;
import jgibblda.LDACmdOption;

public class JGibbsLDAWrapper {
	File f;
	
	public JGibbsLDAWrapper(){
		try {
			f = File.createTempFile("featgen_jgibs", "");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @param docs A stream of docs, where each doc is a stream of words
	 */
	public void getData(Stream<Stream<String>> docs, int nDocs){
		try (FileOutputStream fout = new FileOutputStream(f)){
			PrintWriter pw = new PrintWriter(fout);
			pw.println(nDocs);
			docs.forEach((doc)->{
				boolean[] b = new boolean[1];
				b[0] = true;
				doc.forEach((w)->{
					if(b[0]){
						b[0] = false;
					}else{
						pw.print(" ");
					}
					pw.print(w);
				});
				pw.println();
			});
			pw.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Map<Integer,Map<String,Double>> process(int nTopics){
		LDACmdOption option = new LDACmdOption();

		option.est = true;
		option.inf = false;
		option.K = nTopics;
		option.alpha =  50 / option.K;
		option.beta = 0.1;
		option.niters = 1000;
		option.dir = f.getParentFile().getAbsolutePath();
		option.dfile = f.getName();
		option.twords = 200;

		Estimator estimator = new Estimator();
		estimator.init(option);
		estimator.estimate();
		
		Map<Integer,String> wordIdMap = getWordIdMap();
		return getWordAssignment(wordIdMap);
	}

	public Map<Integer,String> getWordIdMap(){
		Map<Integer,String> map = new HashMap<>();
		File f2 = new File(f.getParent() + "\\" + "wordmap.txt");
		try(FileInputStream fis = new FileInputStream(f2)){
			//Construct BufferedReader from InputStreamReader
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		 
			String line = null;
			line = br.readLine(); // ignore first line
			while ((line = br.readLine()) != null) {
				String[] sp = line.split(" ");
				map.put(Integer.parseInt(sp[1]), sp[0]);
			}
			br.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return map;
	}
	
	public Map<Integer,Map<String,Double>> getWordAssignment(Map<Integer,String> wordIdMap){
		File f2 = new File(f.getParent() + "\\" + "model-final.phi");
		Map<Integer,Map<String,Double>> map = new HashMap<>();
		try(FileInputStream fis = new FileInputStream(f2)){
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String line = null;
			int topicId = 0;
		
			while ((line = br.readLine()) != null) {
				String[] sp = line.split(" ");
				Map<String,Double> m = new HashMap<>();
				for(int i = 0; i < sp.length; i++){
					m.put(wordIdMap.get(i),Double.parseDouble(sp[i]));
				}
				map.put(topicId, m);
				topicId++;				
			}		
			br.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return map;
	}
}
