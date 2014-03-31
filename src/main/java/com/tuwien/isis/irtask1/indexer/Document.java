package com.tuwien.isis.irtask1.indexer;

/**
 * Encapsulation of data about a document
 * 
 * @author Taylor
 */
public class Document {

	/**
	 * Document ID
	 */
	private Integer id;

	/**
	 * Document name
	 */
	private String name;

	/**
	 * Class assigned to document
	 */
	private String classAssignment;

	/**
	 * Create a new document instance
	 * 
	 * @param name
	 * @param id
	 * @param classAssignment
	 */
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
