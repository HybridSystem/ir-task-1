package com.tuwien.isis.irtask1.indexer;

public class Document {

	private Integer id;

	private String name;

	private String classAssignment;

	public Document(String name, Integer id, String classAssignment) {
		this.name = name;
		this.id = id;
		this.classAssignment = classAssignment;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getClassAssignment() {
		return classAssignment;
	}

	public void setClassAssignment(String classAssignment) {
		this.classAssignment = classAssignment;
	}
}
