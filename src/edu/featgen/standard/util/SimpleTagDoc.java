package edu.featgen.standard.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleTagDoc {
	String name;
	String content;
	
	Map<String,List<String>> attrs = new HashMap<String, List<String>>();
	Map<String,List<SimpleTagDoc>> children = new HashMap<String, List<SimpleTagDoc>>();
	
	public SimpleTagDoc(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setContent(String content) {
		this.content = content;
	}
	public String getContent() {
		return content;
	}
	
	public void addAttribute(String name, String value) {
		List<String> attr = attrs.get(name);
		if (attr == null) {
			attr = new ArrayList<String>();
		}
		attr.add(value);
		attrs.put(name, attr);
	}
	public List<String> attrValues(String name) {
		return attrs.get(name) != null ? attrs.get(name) : new ArrayList<String>();
	}
	public String attr(String name, int index) {
		List<String> attr = attrValues(name);
		return attr.size() - 1 < index ? null : attr.get(index);
	}
	public String attr(String name) {
		return attr(name, 0);
	}
	
	public void addChild(SimpleTagDoc child) {
		List<SimpleTagDoc> c = children.get(child.name);
		if (c == null) {
			c = new ArrayList<SimpleTagDoc>();
		}
		c.add(child);
		children.put(child.name, c);
	}
	public List<SimpleTagDoc> children(String name) {
		return children.get(name) != null ? children.get(name) : new ArrayList<SimpleTagDoc>();
	}
	public SimpleTagDoc child(String name, int index) {
		List<SimpleTagDoc> c = children(name);
		return children(name).size() - 1 < index ? null : c.get(index);
	}
	public SimpleTagDoc child(String name) {
		return child(name, 0);
	}
}
