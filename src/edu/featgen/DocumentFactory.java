package edu.featgen;

import edu.featgen.def.Document;
import edu.featgen.impl.DocumentImpl;

public class DocumentFactory {
	private DocumentFactory() {
	}
	static long id = 0;

	public static synchronized Document getDocument() {
		return new DocumentImpl(id++);
	}
}
