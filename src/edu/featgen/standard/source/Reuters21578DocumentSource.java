package edu.featgen.standard.source;

import java.io.FileNotFoundException;
import java.io.FileReader;

import org.joox.Match;

import edu.featgen.DocumentFactory;
import edu.featgen.def.Document;
import edu.featgen.def.DocumentSource;
import edu.featgen.standard.util.SimpleTagDoc;
import edu.featgen.standard.util.SimpleTagDocReader;

public class Reuters21578DocumentSource implements DocumentSource {

	private static final long serialVersionUID = -5689783761956683304L;

	public static final String ID_FEATURE = "id";
	public static final String TITLE_FEATURE = "title";
	public static final String BODY_FEATURE = "body";
	
	protected String directory;
	public String name;
	protected int currentFileIndex;
	protected SimpleTagDocReader reader;

	public Reuters21578DocumentSource(String name,
			String directory) {
		this.name = name;
		this.directory = directory;
		reset();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void reset() {
		currentFileIndex = 0;
		reader = null;
	}

	@Override
	public Document getNext() {
		Document doc;

		if (reader == null) {
			try {
				reader = new SimpleTagDocReader(new FileReader(getCurrentFilePath()));
				currentFileIndex ++;
			} catch (FileNotFoundException e) {
				return null;
			}
		}
		SimpleTagDoc simpleDoc;
		do {
			doc = null;
			simpleDoc = reader.next(null);
			
			if (simpleDoc == null) {
				// try next file
				try {
					reader = new SimpleTagDocReader(new FileReader(getCurrentFilePath()));
				} catch (FileNotFoundException e) {
					return null;
				}
				simpleDoc = reader.next(null);
				currentFileIndex ++;
				if (simpleDoc == null) {
					continue;
				}
			}
			
			// Do some validations
			if (!simpleDoc.getName().equals("REUTERS")) {
				continue;
			}
			
			// Using Modified Apte ("ModApte") Split
			if(simpleDoc.attr("LEWISSPLIT") == null ||
					(!simpleDoc.attr("LEWISSPLIT").equals("TRAIN") && !simpleDoc.attr("LEWISSPLIT").equals("TEST")) ||
					!simpleDoc.attr("TOPICS").equals("YES")) {
				continue;
			}

			String title = simpleDoc.child("TEXT").child("TITLE") == null ? "" :
					simpleDoc.child("TEXT").child("TITLE").getContent();
			String body = simpleDoc.child("TEXT").child("BODY") == null ? "" :
				simpleDoc.child("TEXT").child("BODY").getContent();
			
			// Create a document object
			doc = DocumentFactory.getDocument();
			
			doc.getSourceFeatureSet().add(ID_FEATURE, new Integer(simpleDoc.attr("NEWID")));
			doc.getSourceFeatureSet().add(TITLE_FEATURE, title);
			doc.getSourceFeatureSet().add(BODY_FEATURE, body);
			doc.setTraining(simpleDoc.attr("LEWISSPLIT").equals("TRAIN"));

			
			// add expected topics
			if (simpleDoc.child("TOPICS") != null && 
					!simpleDoc.child("TOPICS").children("D").isEmpty()) {
				for(SimpleTagDoc child : simpleDoc.child("TOPICS").children("D")) {
					doc.getClasses().add(child.getContent());
				}
			}
		} while (doc == null);

		return doc;
	}

	protected String getCurrentFilePath() {
		return directory + "/reut2-0" + (currentFileIndex < 10 ? "0" : "") + 
				new Integer(currentFileIndex).toString() + ".sgm";
	}

	public static DocumentSource instantiateFromXML(Match conf) {
		return new Reuters21578DocumentSource(conf.attr("name"),conf.xpath("./Directory").content().trim());
	}

	@Override
	public String summary(Document doc){
		String title = doc.getSourceFeatureSet().getString(TITLE_FEATURE);
		if(title.length() > 100){
			title = title.substring(0, 100);
		}
		String body = doc.getSourceFeatureSet().getString(BODY_FEATURE);
		if(body.length() > 100){
			body = body.substring(0, 100);
		}
		return "id: " + doc.getSourceFeatureSet().get(ID_FEATURE) +
				"; title: " + title + "; body: " + body;
	}
	
}