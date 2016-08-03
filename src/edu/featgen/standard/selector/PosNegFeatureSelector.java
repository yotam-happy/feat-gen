package edu.featgen.standard.selector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.joox.Match;

import edu.featgen.def.DocumentSet;
import edu.featgen.def.FeatureSelector;
import edu.featgen.standard.util.InfoGainCalc;
import edu.wiki.util.Tuple;

public class PosNegFeatureSelector  implements FeatureSelector{
	private static final long serialVersionUID = -1105308652606726149L;

	Set<String> sourceFeatureSets;
	public String name;
	private int keepNFeatures;
	InfoGainCalc.EstimationEnum type;
	
	public PosNegFeatureSelector(String name,
			Set<String> sourceFeatureSets,
			int keepNFeatures,
			String posNeg) {
		this.name = name;
		this.sourceFeatureSets = sourceFeatureSets;
		this.keepNFeatures = keepNFeatures;
		if (posNeg.toLowerCase().equals("pos")){
			type = InfoGainCalc.EstimationEnum.POS;
		} else if (posNeg.toLowerCase().equals("neg")){
			type = InfoGainCalc.EstimationEnum.NEG;
		} else {
			throw new RuntimeException("value not supported");
		}
	}

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public Set<String> getFilteredFeatures(DocumentSet docs, String targetClass){
		Set<String> best = getBestFeatures(docs, targetClass, sourceFeatureSets, keepNFeatures, null, type);
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
	
	public static Set<String> getBestFeatures(
			DocumentSet docs, 
			String targetClass,
			Set<String> sourceFeatureSets,
			int keepNFeatures,
			Set<String> chooseFrom,
			InfoGainCalc.EstimationEnum type){
		
		List<Tuple<Double,String>> l = InfoGainFeatureSelector.getFeaturesRelevance(
				docs, targetClass, sourceFeatureSets, chooseFrom,
				()-> new InfoGainCalc(2, false, null, type));
		
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
		return new PosNegFeatureSelector(conf.attr("name"),
				sourceFeatureSets,
				Integer.parseInt(conf.xpath("./KeepNFeatures").content().trim()),
				conf.xpath("./PosNeg").content().trim());
	}

}
