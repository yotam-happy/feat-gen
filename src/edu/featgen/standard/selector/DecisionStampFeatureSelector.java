package edu.featgen.standard.selector;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.joox.Match;

import edu.featgen.def.DocumentSet;
import edu.featgen.def.FeatureSelector;
import edu.featgen.standard.util.FeatureRelevanceMeasure;
import edu.featgen.standard.util.InfoGainCalc;
import edu.featgen.standard.util.Logger;
import edu.wiki.util.Tuple;
import edu.wiki.util.db.ArticleTitleQueryOptimizer;

public class DecisionStampFeatureSelector  implements FeatureSelector{
	private static final long serialVersionUID = -8113995884102242493L;

	Set<String> sourceFeatureSets;
	public String name;
	private int keepNFeatures;

	public DecisionStampFeatureSelector(String name,
			Set<String> sourceFeatureSets,
			int keepNFeatures,
			boolean focusOn) {
		this.name = name;
		this.sourceFeatureSets = sourceFeatureSets;
		this.keepNFeatures = keepNFeatures;
	}

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public Set<String> getFilteredFeatures(DocumentSet docs, String targetClass){
		Set<String> best = getBestFeatures(docs, targetClass, sourceFeatureSets, keepNFeatures, null);
		Set<String> res = new HashSet<>();
		docs.forEach((docId, doc)->{
			doc.featureSetStream().filter((fs)->fs.isUsedForClassification() && sourceFeatureSets.contains(fs.getName()))
				.flatMap((fs)->fs.stream())
				.forEach((f)->{
					if(!best.contains(f.getName()) && !res.contains(f.getName())){
						res.add(f.getName());
					}
				});
		});
		return res;
	}
	
	public static String ESAArticleName(String n){
		int id = Integer.parseInt(n.substring(n.lastIndexOf('_') + 1));
		String s = ArticleTitleQueryOptimizer.getInstance().doQuery(id);
		if (s.length() > 25){
			s = s.substring(0, 25);
		}
		return s;
	}
	public static String d2s(double d){
		String s = Double.toString(d);
		return s.length() <= 7 ? s : s.substring(0, 7); 
	}
	public static void reportForFeatures(Set<String> features,
			DocumentSet docs, 
			String targetClass){
		double[] expectedWeightPerLabel = new double[2];
		// class counts
		docs.forEach((docId, doc)->{
			int cls = doc.getClasses().contains(targetClass) ? 1 : 0;
			expectedWeightPerLabel[cls] += 1;
		});

		Map<String,FeatureRelevanceMeasure> featureRelevance = new HashMap<>(); 
		features.forEach((name)->{
			FeatureRelevanceMeasure c = new InfoGainCalc(2, null);
			c.setExpectedWeightPerLabelHint(expectedWeightPerLabel);
			featureRelevance.put(name, c);
		});

		// get feature statistics
		docs.forEach((docId, doc)->{
			int cls = doc.getClasses().contains(targetClass) ? 1 : 0;
			doc.featureSetStream().filter((fs)->fs.isUsedForClassification())
			.flatMap((fs)->fs.stream()).forEach((f)->{
				if(featureRelevance.containsKey(f.getName())){
					featureRelevance.get(f.getName()).addSample(cls, f.doubleValue());
				}
			});
		});
		
		// calculate final info gains
		List<Tuple<Double,String>> l = featureRelevance.entrySet().stream()
			.map((e)->{
				e.getValue().estimateRelevance();
				return new Tuple<Double,String>(e.getValue().estimateRelevanceForLabel(0), e.getKey());
			})
			.sorted((e1,e2)->-Double.compare(e1.x.doubleValue(),e2.x.doubleValue()))
			.collect(Collectors.toList());
		
		Collections.sort(l, (e1,e2)->-Double.compare(e1.x.doubleValue(),e2.x.doubleValue()));

		Logger.writeToReport("");
		Logger.writeToReport("Feature Relevance estimates for category: " + targetClass);
		Logger.writeToReport("----------------------------------------------------");
		Logger.writeToReport("");

		for(Tuple<Double,String> f : l){
			FeatureRelevanceMeasure c = featureRelevance.get(f.y);
			StringBuffer sb = new StringBuffer();
			String n = f.y;
			if(n.startsWith("esa")){
				n = ESAArticleName(n);
			}
			sb.append("feature stats for: ").append(n)
			.append(" info gain: ").append(d2s(f.x))
			.append(" negative cont.:" )
			.append(d2s(c.estimateRelevanceForLabel(0)))
			.append(" positive cont.:" ).append(d2s(c.estimateRelevanceForLabel(1)));
			Logger.writeToReport(sb.toString());
		}
	}

	public static Set<String> getBestFeatures(
			DocumentSet docs, 
			String targetClass,
			Set<String> sourceFeatureSets,
			int keepNFeatures,
			Set<String> chooseFrom){
		
		double[] expectedWeightPerLabel = new double[2];
		// class counts
		docs.forEach((docId, doc)->{
			int cls = doc.getClasses().contains(targetClass) ? 1 : 0;
			expectedWeightPerLabel[cls] += 1;
		});
		
		// get all features
		Map<String,FeatureRelevanceMeasure> featureRelevance = new HashMap<>(); 
		docs.forEach((docId, doc)->{
			doc.featureSetStream().filter((fs)->fs.isUsedForClassification() && sourceFeatureSets.contains(fs.getName()))
				.flatMap((fs)->fs.stream())
				.forEach((f)->{
					if(chooseFrom == null || chooseFrom.contains(f.getName())){
						if(!featureRelevance.containsKey(f.getName())){
							FeatureRelevanceMeasure c = new InfoGainCalc(2, null);
							c.setExpectedWeightPerLabelHint(expectedWeightPerLabel);
							featureRelevance.put(f.getName(), c);
						}
					}
				});
		});
		
		System.out.println("Doing infogain feature selection with " + featureRelevance.size() + " features");

		// get feature statistics
		docs.forEach((docId, doc)->{
			int cls = doc.getClasses().contains(targetClass) ? 1 : 0;
			doc.featureSetStream().filter((fs)->fs.isUsedForClassification() && sourceFeatureSets.contains(fs.getName()))
			.flatMap((fs)->fs.stream()).forEach((f)->{
				if(chooseFrom == null || chooseFrom.contains(f.getName())){
					featureRelevance.get(f.getName()).addSample(cls, f.doubleValue());
				}
			});
		});
		
		// calculate final info gains
		List<Tuple<Double,String>> l = featureRelevance.entrySet().stream()
			.map((e)->new Tuple<Double,String>(e.getValue().estimateRelevance(), e.getKey()))
			.sorted((e1,e2)->-Double.compare(e1.x.doubleValue(),e2.x.doubleValue()))
			.collect(Collectors.toList());
		
		Collections.sort(l, (e1,e2)->-Double.compare(e1.x.doubleValue(),e2.x.doubleValue()));
		// get filtered
		Set<String> res = new HashSet<>();
		int i = 0;
		for(Tuple<Double,String> f : l){
			if (i < keepNFeatures){
				res.add(f.y);
			}
			i++;
		}
		return res;
	}
	
	public static FeatureSelector instantiateFromXML(Match conf) {
		Set<String> sourceFeatureSets = conf.xpath("./SourceFeatureSet").contents()
				.stream().map((s)->s.trim()).collect(Collectors.toSet());
		return new DecisionStampFeatureSelector(conf.attr("name"),
				sourceFeatureSets,
				Integer.parseInt(conf.xpath("./KeepNFeatures").content().trim()),
				conf.xpath("./KeepNFeatures").content().trim().equalsIgnoreCase("positive"));
	}

}
