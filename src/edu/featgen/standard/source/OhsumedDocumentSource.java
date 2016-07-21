package edu.featgen.standard.source;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.joox.Match;

import edu.featgen.DocumentFactory;
import edu.featgen.def.Document;
import edu.featgen.def.DocumentSource;

public class OhsumedDocumentSource implements DocumentSource {
	private static final long serialVersionUID = 4940906663299181163L;

	public static final String ID_FEATURE = "id";
	public static final String TITLE_FEATURE = "title";
	public static final String ABSTRACT_FEATURE = "abs";

	protected String filename;
	protected boolean only20000Docs;
	protected boolean onlyShort;
	public String name;
	protected BufferedReader br;
	protected int nextId;
	protected int nRetrieved;

	public OhsumedDocumentSource(String name,
			String filename,
			boolean onlyShort,
			boolean only20000Docs) {	// following Joachims (1998)
		this.name = name;
		this.filename = filename;
		this.only20000Docs = only20000Docs;
		this.onlyShort = onlyShort;
		reset();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void reset() {
		try {
			br = new BufferedReader(new FileReader(filename));
			String l = br.readLine();
			nextId = Integer.valueOf(l.substring(3).trim());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		nRetrieved = 0;
	}

	@Override
	public Document getNext() {
		Document doc;

		if (br == null) {
			return null;
		}
		
		do {
			doc = null;
			// Read a document
			Map<String,String> props = new HashMap<>();
			int id = 0;
			try {
				String l = br.readLine();
				while(l != null && !l.startsWith(".I")){
					if(!l.startsWith(".") || l.length() != 2){
						throw new RuntimeException("File does not match OHSUMED expected format");
					}
					props.put(l.substring(1,2), br.readLine().trim());
					l = br.readLine();
				}
				if (l == null){
					br.close();
					br = null;
					return null;
				}

				id = nextId;
				nextId = Integer.valueOf(l.substring(3).trim());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (onlyShort && props.containsKey("W") && !props.get("W").isEmpty()){
				continue;
			}
			if (only20000Docs && (nRetrieved > 20000 || !props.get("U").substring(0,2).equals("91"))){
				continue;
			}

			// Create a document object
			doc = DocumentFactory.getDocument();
			
			doc.getSourceFeatureSet().add(ID_FEATURE, id);
			if (only20000Docs){
				doc.setTraining(nRetrieved < 10000);
			}
			doc.getSourceFeatureSet().add(TITLE_FEATURE, props.get("T"));
			doc.getSourceFeatureSet().add(ABSTRACT_FEATURE, props.get("W"));

			
			// add expected topics
			if ( props.get("M") != null){
				String[] topics = props.get("M").split(";");
				for(String topic : topics) {
					if(topic.contains("/")){
						topic = topic.substring(0,topic.indexOf('/'));
					}
					while(topic.endsWith(".")){
						topic = topic.substring(0, topic.length() - 1);
					}
					doc.getClasses().add(topic.trim());
				}
			}
		} while (doc == null);

		nRetrieved++;
		return doc;
	}

	public static DocumentSource instantiateFromXML(Match conf) {
		return new OhsumedDocumentSource(conf.attr("name"),
				conf.xpath("./Filename").content().trim(),
				Boolean.valueOf(conf.xpath("./Only20000Docs").content().trim()),
				Boolean.valueOf(conf.xpath("./OnlyShort").content().trim()));
	}

	@Override
	public String summary(Document doc){
		String title = doc.getSourceFeatureSet().getString(TITLE_FEATURE);
		if(title.length() > 100){
			title = title.substring(0, 100);
		}
		if(title == null){
			title = "---";
		}
		String abst = doc.getSourceFeatureSet().getString(ABSTRACT_FEATURE);
		if(abst == null){
			abst = "---";
		}
		if(abst.length() > 100){
			abst = abst.substring(0, 100);
		}
		return "id: " + doc.getSourceFeatureSet().get(ID_FEATURE) +
				"; title: " + title + "; abstract: " + abst;
	}
}
