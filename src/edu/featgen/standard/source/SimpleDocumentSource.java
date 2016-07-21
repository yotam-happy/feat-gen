package edu.featgen.standard.source;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.joox.Match;

import edu.featgen.DocumentFactory;
import edu.featgen.def.Document;
import edu.featgen.def.DocumentSource;

/**
 * reads text files in the form: [test/train]:<class>:<text>
 *
 */
public class SimpleDocumentSource implements DocumentSource{
	private static final long serialVersionUID = -2902001828352963452L;

	public static final String BODY_FEATURE = "body";

	private String name;
	private String filename;
	protected BufferedReader br;

	public SimpleDocumentSource(String name,
			String filename) {
		this.name = name;
		this.filename = filename;
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
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Document getNext() {
		Document doc;

		if (br == null) {
			return null;
		}
		String[] arr;
		try {
			String l = br.readLine();
			if (l == null){
				br.close();
				br = null;
				return null;
			}
			arr = l.split(":",3);
			if (arr.length != 3){
				throw new RuntimeException("Source file not in right format");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		doc = DocumentFactory.getDocument();
		
		doc.getSourceFeatureSet().add(BODY_FEATURE, arr[2].trim());
		doc.setTraining(arr[0].trim().equals("train"));
		doc.getSourceFeatureSet().add(arr[1].trim(), new Boolean(true));

		return doc;
	}

	public static DocumentSource instantiateFromXML(Match conf) {
		return new SimpleDocumentSource(conf.attr("name"),
				conf.xpath("./Filename").content().trim());
	}

	@Override
	public String summary(Document doc){
		String body = doc.getSourceFeatureSet().getString(BODY_FEATURE);
		if(body.length() > 100){
			body = body.substring(0, 100);
		}
		return "body: " + body;
	}
}
