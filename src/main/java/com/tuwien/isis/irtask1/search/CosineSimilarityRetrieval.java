package com.tuwien.isis.irtask1.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Similarity Retrieval via the Vector Space Model and cosine functions as a
 * distance function
 * 
 * @author Wagi
 * 
 */
public class CosineSimilarityRetrieval {

	/**
	 * @param queryDocVec
	 * @return list with all documents with calculated cosines > 0
	 */
	public static List<DocumentVector> buildSearchResultsList(
			DocumentVector queryDocVec, List<DocumentVector> documentVectors) {
		List<DocumentVector> resultList = new ArrayList<DocumentVector>();
		
		for (DocumentVector currDocVec: documentVectors){
			
			double cosine = calculateCosine(queryDocVec,
					currDocVec);
			
			if (cosine != 0
					&& (currDocVec.getDocumentData().getId() != queryDocVec
							.getDocumentData().getId())) {
				currDocVec.setCosine(cosine);
				resultList.add(currDocVec);
			}
		}
		return resultList;
	}

	/**
	 * calculates cosine between 2 input vectors: DotProduct(vec1, vec2) /
	 * (length(vec1) * length(vec2))
	 * 
	 * @param docVec1
	 * @param docVec2
	 * @return
	 */
	private static double calculateCosine(DocumentVector docVec1,
			DocumentVector docVec2) {

		double cosine = 0;

		double denominator = calculateVectorLength(docVec1)
				* calculateVectorLength(docVec2);

		if (denominator != 0) {
			cosine = calculateVectorDotProduct(docVec1, docVec2) / denominator;
		}

		// System.out.println("calcd cosined: " + cosine);
		return cosine;
	}

	private static double calculateVectorDotProduct(DocumentVector docVec1,
			DocumentVector docVec2) {
		double sum = 0;

		Set<Entry<String, Float>> vec1 = docVec1.getDocIdftfMap().entrySet();
		Map<String, Float> vec2 = docVec2.getDocIdftfMap();

		for (Map.Entry<String, Float> entry : vec1) {
			if (vec2.containsKey(entry.getKey())) {
				sum += entry.getValue() * vec2.get(entry.getKey());
				// System.out.println("- clac " + entry.getKey() +": sum += " +
				// entry.getValue() + " * " + vec2.get(entry.getKey()));
			}
		}

		// System.out.println("sum: " + sum);

		return sum;
	}

	private static double calculateVectorLength(DocumentVector docVec1) {
		double sum = 0;

		Set<Entry<String, Float>> entrySet = docVec1.getDocIdftfMap()
				.entrySet();

		for (Map.Entry<String, Float> entry : entrySet) {
			sum += Math.pow(entry.getValue(), 2);
		}

		return Math.sqrt(sum);
	}
}
