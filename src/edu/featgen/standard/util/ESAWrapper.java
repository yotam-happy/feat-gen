package edu.featgen.standard.util;

import java.util.HashMap;

import edu.wiki.api.concept.IConceptVector;
import edu.wiki.search.ESAMultiResolutionSearcher;
import edu.wiki.search.ESASearcher;
import edu.wiki.search.ESASearcherCategories;

/**
 * Not thread safe. (threadsafing this could be as simple as synchronizing
 * getInstance, but that depends on weather ESAlib is thread safe itself.) 
 */
public class ESAWrapper {
	
	private static ESAWrapper instance = null;
	private ESAMultiResolutionSearcher searcher;
	private ESASearcherCategories categoriesSearcher;
	
	public static ESAWrapper getInstance() {
		if (instance == null) {
			instance = new ESAWrapper();
		}
		return instance;
	}

	private ESAWrapper() {
		searcher = new ESAMultiResolutionSearcher();
		categoriesSearcher = new ESASearcherCategories();
//		Concept2ndOrderQueryOptimizer.getInstance().loadAll();
//		TermQueryOptimizer.getInstance().loadAll();
//		IdfQueryOptimizer.getInstance().loadAll();
	}

	public IConceptVector getMultiResolutionVector(String doc, int limit){
		return searcher.getConceptVectorUsingMultiResolution2(doc, limit, true, true);
	}
	public HashMap<String,Integer> getBOW(String doc, boolean stemming){
		return searcher.getBOW(doc, stemming);
	}
	public IConceptVector getConceptVector(String doc, int limit){
		return ESASearcher.getNormalVector(searcher.getConceptVector(doc), limit);
	}
	public IConceptVector get2ndOrderConceptVector(String doc, int limit){
		return searcher.getCombinedVector(doc, limit);
	}
	public IConceptVector getCategoriesVector(IConceptVector v, int limit){
		return ESASearcher.getNormalVector(categoriesSearcher.getCategoriesVector(v),limit);
	}
}
