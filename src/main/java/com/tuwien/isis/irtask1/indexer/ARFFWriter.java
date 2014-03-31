package com.tuwien.isis.irtask1.indexer;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

public class ARFFWriter {

	public static void writeIndexToFile(Indexer informationRetriever) throws Exception {
		Set<String> classAssignmentSet = informationRetriever.getClassAssignmentSet();
		ArrayList<Document> documentsList = informationRetriever.getDocumentsList();
		Map<String, Map<Integer, Float>> tokenIdftfMap = informationRetriever.getTokenIdfTfMap();
		ArrayList<String> sortedTokenList = informationRetriever.getSortedTokenList();

		FastVector attributes = new FastVector();

		addAttributeFileName(attributes);
		addAttributeDocumentID(attributes);
		FastVector classAssignmentValues = addAttributeClassAssignment(classAssignmentSet, attributes);

		int tokenAttributesCount = addAttributesTokens(sortedTokenList, attributes);

		Instances instancesData = new Instances("Index", attributes, 0);
		addDocumentData(documentsList, instancesData, classAssignmentValues, tokenIdftfMap, tokenAttributesCount);

		ArffSaver saver = new ArffSaver();
		saver.setInstances(instancesData);
		saver.setFile(new File("index.arff"));
		saver.writeBatch();
	}

	private static void addDocumentData(ArrayList<Document> documentsList, Instances instancesData,
			FastVector classAssignmentValues, Map<String, Map<Integer, Float>> tokenIdftfMap, int tokenAttributesCount) {
		int documentsCount = documentsList.size();
		for (int i = 0; i < documentsCount; ++i) {
			double[] dataValues = new double[instancesData.numAttributes()];
			Document currentDocumentData = documentsList.get(i);

			dataValues[0] = instancesData.attribute(0).addStringValue(currentDocumentData.getName());
			dataValues[1] = currentDocumentData.getId();
			dataValues[2] = classAssignmentValues.indexOf(currentDocumentData.getClassAssignment());

			for (int k = 0; k < tokenAttributesCount; ++k) {
				// Get IDFTF Map of the current token
				Map<Integer, Float> docIdftfMap = tokenIdftfMap.get(instancesData.attribute(3 + k).name());

				if (docIdftfMap == null)
					dataValues[3 + k] = 0;
				else {
					Float value = docIdftfMap.get(currentDocumentData.getId());

					if (value != null)
						dataValues[3 + k] = value;
					else
						dataValues[3 + k] = 0;
				}
			}

			instancesData.add(new Instance(1.0, dataValues));
		}
	}

	private static void addAttributeFileName(FastVector attributes) {
		attributes.addElement(new Attribute("File name", (FastVector) null));
	}

	private static void addAttributeDocumentID(FastVector attributes) {
		attributes.addElement(new Attribute("Document ID"));
	}

	private static FastVector addAttributeClassAssignment(Set<String> classAssignmentSet, FastVector attributes) {
		FastVector classAssignmentValues;
		classAssignmentValues = new FastVector();
		Iterator<String> it = classAssignmentSet.iterator();
		while (it.hasNext()) {
			String value = it.next();
			classAssignmentValues.addElement(value);
		}

		attributes.addElement(new Attribute("Class Assignment", classAssignmentValues));

		return classAssignmentValues;
	}

	/**
	 * Adds the tokens as attributes
	 * 
	 * @param sortedTokenList
	 *            - sorted list of tokens
	 * @param attributes
	 *            - list of attributes
	 * @return count of tokens
	 */
	private static int addAttributesTokens(ArrayList<String> sortedTokenList, FastVector attributes) {
		int tokenCount = sortedTokenList.size();

		for (int i = 0; i < tokenCount; ++i) {
			String value = sortedTokenList.get(i);
			attributes.addElement(new Attribute(value));
		}

		return tokenCount;
	}
}
