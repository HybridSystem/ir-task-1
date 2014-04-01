package com.tuwien.isis.irtask1.search;

import java.util.Comparator;

/**
 * Comparator for sorting DocumentVectors by their Cosine values
 * @author Wagi
 *
 */
public class DocumentVectorCosineComparator implements Comparator<DocumentVector>{

	public int compare(DocumentVector v1, DocumentVector v2) {
		if (v1.getCosine() < v2.getCosine()) {
			return 1;
		} else if (v1.getCosine() > v2.getCosine()) {
			return -1;
		} else
			return 0;
	}
}
