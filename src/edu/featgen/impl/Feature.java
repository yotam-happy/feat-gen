package edu.featgen.impl;

public class Feature{
	public final String name;
	public final Object value;
	
	public Feature(String name, Object value) {
		this.name = name;
		this.value = value;
	}
	
	public String getName() {
		return name;
	}
	
	public Object getValue() {
		return value;
	}
	
	public String stringValue() {
		return (String)value;
	}

	public Double doubleValue() {
		return (Double)value;
	}

	public Boolean booleanValue() {
		return (Boolean)value;
	}
}
