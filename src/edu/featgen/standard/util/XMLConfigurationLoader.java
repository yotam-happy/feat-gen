package edu.featgen.standard.util;

import static org.joox.JOOX.$;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.joox.Match;
import org.xml.sax.SAXException;

import edu.featgen.Documents;
import edu.featgen.Experiment;
import edu.featgen.def.DocumentSource;
import edu.featgen.def.FeatureGenerator;
import edu.featgen.def.FeatureSelector;


public class XMLConfigurationLoader {
	
	private XMLConfigurationLoader() {
	}
	
	public static Experiment buildFromFile(File f) {
		Documents docs = new Documents();
		Match confs;

		try {
			confs = $(f).xpath("/FeatGen").first();
		} catch (SAXException | IOException e) {
			throw new RuntimeException(e);
		}
		
		// Load document Source
		DocumentSource documentSource = (DocumentSource)instantiateObjectFromXML(confs.xpath("./DocumentSource").first());
		Set<FeatureGenerator> generators = new HashSet<>();
		Set<FeatureSelector> selectors = new HashSet<>();
		confs.xpath("./Generators/FeatureGenerator").forEach((elem)->generators.add((FeatureGenerator)instantiateObjectFromXML($(elem))));
		confs.xpath("./FeatureSelectors/FeatureSelector").forEach((elem)->selectors.add((FeatureSelector)instantiateObjectFromXML($(elem))));

		docs.setDocumentSource(documentSource);
		generators.forEach((gen)->docs.addFeatureGenerator(gen));
		selectors.forEach((sel)->docs.addFeatureSelector(sel));
		
		Experiment experiment = Experiment.instantiateFromXML(confs.xpath("./Experiment"));
		experiment.setDocuments(docs);
		return experiment;
	}
	
	public static Object instantiateObjectFromXML(Match conf) {
		try {
			Class<?> objClass = Class.forName(conf.attr("class"));
			Method factory = objClass.getMethod("instantiateFromXML", Match.class);
			return factory.invoke(null, conf);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}