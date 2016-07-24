package edu.featgen.standard.generator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.joox.Match;

import edu.featgen.def.Document;
import edu.featgen.def.DocumentSet;
import edu.featgen.def.FeatureGenerator;
import edu.featgen.standard.util.ESAWrapper;
import edu.wiki.api.concept.IConceptVector;
import edu.wiki.util.db.ArticleLengthQueryOptimizer;
import edu.wiki.util.db.ArticleTitleQueryOptimizer;

public class ESAFeatureGenerator implements FeatureGenerator{
	private static final long serialVersionUID = -6962479222584079426L;

	public static final String FEATURE_SET = "esa";
	public static final String CATEGORY_FEATURE_SET = "esacats";
	
	ESAWrapper esa;
	
	private String sourceFeatureGenerator;
	private String sourceFeatureName;
	public String name;
	private boolean usedForClassification;
	private boolean categories;
	private final int maxConeceptsPerDoc = 200;

	public ESAFeatureGenerator(String name,
			String sourceFeatureGenerator,
			String sourceFeatureName,
			boolean categories,
			boolean usedForClassification) {
		this.name = name;
		this.sourceFeatureGenerator = sourceFeatureGenerator;
		this.sourceFeatureName = sourceFeatureName;
		this.categories = categories;
		this.usedForClassification = usedForClassification;
		esa = ESAWrapper.getInstance();
		ArticleLengthQueryOptimizer.getInstance().loadAll();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Set<String> getRequiredInputs() {
		return new HashSet<>(Arrays.asList(sourceFeatureGenerator));
	}

	@Override
	public Object preProcess(DocumentSet docs, Object transientData) {
		return null;
	}

	@Override
	public void processDocument(Document doc) {
		doc.addFeatureSet(FEATURE_SET, usedForClassification);
		String text = doc.getFeature(sourceFeatureName).stringValue();
		
		IConceptVector concepts = esa.getMultiResolutionVector(text, maxConeceptsPerDoc);
		
		if (concepts == null){
			return;
		}
		concepts.forEach((id,s)->
			doc.getFeatureSet(FEATURE_SET).add(Integer.toString(id), (Object)s));
		
		if (categories){
			processCategories(doc, concepts);
		}
	}
	
	protected void processCategories(Document doc, IConceptVector concepts){
		doc.addFeatureSet(CATEGORY_FEATURE_SET, usedForClassification);
		IConceptVector categories = esa.getCategoriesVector(concepts, maxConeceptsPerDoc);
		categories.forEach((id,s)->
			doc.getFeatureSet(CATEGORY_FEATURE_SET).add(Integer.toString(id), (Object)s));
	}

	public static FeatureGenerator instantiateFromXML(Match conf) {
		return new ESAFeatureGenerator(conf.attr("name"),
				conf.xpath("./SourceFeatureGenerator").content().trim(),
				conf.xpath("./SourceFeatureName").content().trim(),
				Boolean.parseBoolean(conf.xpath("./Categories").content().trim()),
				Boolean.parseBoolean(conf.xpath("./UsedForClassification").content().trim()));
	}

	@Override
	public void reset(DocumentSet docs, String className) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public boolean isRecalculateForEachClass() {
		return false;
	}

	public static int featureNameToId(String featureName){
		return featureName.contains("_") ?
				Integer.parseInt(featureName.substring(featureName.lastIndexOf('_') + 1)) :
				Integer.parseInt(featureName);
	}
	
	public static String articleName(String n){
		int id = Integer.parseInt(n);
		String s = ArticleTitleQueryOptimizer.getInstance().doQuery(id);
		if (s.length() > 25){
			s = s.substring(0, 25);
		}
		return s;
	}

	@Override
	public String getFeatureDescription(String featureName){
		return null;
	}
	@Override
	public String getFeatureDescriptiveName(String featureName){
		return articleName(featureName);
	}
}
