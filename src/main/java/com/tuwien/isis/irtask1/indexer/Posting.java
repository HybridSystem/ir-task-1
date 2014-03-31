package com.tuwien.isis.irtask1.indexer;

/**
 * Encapsulation of data about a posting
 * 
 * @author Taylor
 */
public class Posting {

	/**
	 * Posting id
	 */
	private int id;

	/**
	 * Posting position
	 */
	private int position;

	public Posting(int id, int position) {
		this.id = id;
		this.position = position;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

}
