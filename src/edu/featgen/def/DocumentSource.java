package edu.featgen.def;

import java.io.Serializable;

public interface DocumentSource extends Serializable {
	String getName();
	void reset();
	Document getNext();
	
	String summary(Document doc);
}
