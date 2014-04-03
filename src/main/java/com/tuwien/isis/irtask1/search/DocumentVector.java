package com.tuwien.isis.irtask1.search;

import java.util.Map;

import com.tuwien.isis.irtask1.indexer.Document;

/**
 * Vector with the documentData, the IDF-TF map and cosin value
 * 
 * @author Wagi
 */
public class DocumentVector {
	private Document documentData;
	private Map<String, Float> docIdftfMap;
	private double cosine;

	public DocumentVector(Document documentData, Map<String, Float> docIdftfMap) {
		this.documentData = documentData;
		this.docIdftfMap = docIdftfMap;
		this.cosine = 0;
	}

	public double getCosine() {
		return cosine;
	}

	public void setCosine(double cosine) {
		this.cosine = cosine;
	}

	public Document getDocumentData() {
		return documentData;
	}

	public String getFullDocName() {
		return documentData.getClassAssignment() + "/" + documentData.getName();
	}

	public void setDocumentData(Document documentData) {
		this.documentData = documentData;
	}

	public Map<String, Float> getDocIdftfMap() {
		return docIdftfMap;
	}

	public void setDocIdftfMap(Map<String, Float> docIdftfMap) {
		this.docIdftfMap = docIdftfMap;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "DocumentVector [documentData=" + documentData
				+ ", docIdftfMap=" + docIdftfMap + ", cosine=" + cosine + "]";
	}

}
