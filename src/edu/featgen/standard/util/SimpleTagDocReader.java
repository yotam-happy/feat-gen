package edu.featgen.standard.util;

import java.io.IOException;
import java.io.Reader;
import java.util.StringTokenizer;

/**
 * Extremely simple tag based document reader.
 * Works for reading Reuters-21578 corpus files
 * @author yotamesh
 *
 */
public class SimpleTagDocReader {
	Reader reader;
	StringBuffer content = null;
	
	public SimpleTagDocReader(Reader reader) {
		this.reader = reader;
	}
	
	public SimpleTagDoc next(String tag) {
		SimpleTagDoc o = null;
		if (tag == null) {
			do {
				tag = nextTagRawText();
				if (tag == null) {
					return null;
				}
				// ignore <!...> tags like DOCTYPE or empty invalid tags
			} while(tag.startsWith("!"));
		}
		
		o = parseTagRawText(tag);
		
		// Read tag content
		StringBuffer wrapperContent = content;
		content = new StringBuffer();
		tag = nextTagRawText();
		while (!tag.equals("/" + o.getName())) {
			SimpleTagDoc child = next(tag);
			if (child != null) {
				o.addChild(child);
			}
			tag = nextTagRawText();
		};
		
		o.setContent(fixContent(content.toString()));
		content = wrapperContent;
		
		return o;
	}
	
	protected SimpleTagDoc parseTagRawText(String tag) {
		StringTokenizer tokenizer = new StringTokenizer(tag);
		SimpleTagDoc o = new SimpleTagDoc(tokenizer.nextToken());
		
		if (tokenizer.hasMoreTokens()) {
			while (tokenizer.hasMoreTokens()) {
				String attr = tokenizer.nextToken();
				String attrName = attr.substring(0, attr.indexOf('='));
				String attrValue = attr.substring(attr.indexOf('=') + 1);
				attrValue = attrValue.substring(1, attrValue.length() - 1); // Expecting the value to be surrounded by '"'
				o.addAttribute(attrName, attrValue);
			}
		}
		return o;
	}
	
	// finds next tag and reads it
	protected String nextTagRawText() {
		try {
			StringBuffer tag;

			// find next tag
			int c = reader.read();
			while (c != '<') {
				if (c == -1) {
					return null;
				}
				if (content != null) {
					content.append((char)c);
				}
				c = reader.read();
			}
			
			// read tag
			tag = new StringBuffer();
			c = reader.read();
			while (c != '>') {
				tag.append((char)c);
				c = reader.read();
			}
			return tag.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected String fixContent(String content) {
		StringBuffer out = new StringBuffer();
		int i = 0;
		int j = content.indexOf('&', i);
		while (j >= 0){
			out.append(content.substring(i, j));
			int k = content.indexOf(';', j);
			i = k + 1;
			String s = content.substring(j+1, k);
			if (s.startsWith("#")) {
				char c = (char)new Integer(s.substring(1)).intValue();
				out.append(c);
			} else {
				switch (s) {
				case "lt":
					out.append('<');
					break;
				case "gt":
					out.append('>');
					break;
				case "amp":
					out.append('&');
					break;
				default:
					throw new RuntimeException("Can't cipher &" + s + "; - probably my mistake");
				}
			}
			j = content.indexOf('&', i);
		}
		out.append(content.substring(i));
		return out.toString().trim();
	}
}
