package com.tuwien.isis.irtask1.search;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader.ArffReader;

import com.tuwien.isis.irtask1.indexer.Document;

/**
 * builds DocumentVectors from ARFF file
 * 
 * @author Wagi
 * 
 */
public class DocumentVectorBuilder {

	public static List<DocumentVector> buildDocumentVectors(String inputFilePath) {
		System.out.println("Starting to build DocumentVectors from ARFF - "
				+ inputFilePath);

		try {
			BufferedReader reader = new BufferedReader(new FileReader(
					inputFilePath));
			ArffReader arff = new ArffReader(reader, 1000);
			// Instances data = arff.getData();

			Instances data = arff.getStructure();
			data.setClassIndex(data.numAttributes() - 1);
			Instance inst;
			while ((inst = arff.readInstance(data)) != null) {
				data.add(inst);
			}

			System.out
					.println("ARRF read, continuing building DocumentVectors");

			System.out.println("DEBUG, class index: " + data.classIndex());

			List<DocumentVector> documentVectors = new ArrayList<DocumentVector>();

			for (int i = 0; i < data.numInstances(); i++) {

				Instance currInstance = data.instance(i);

				String docName = null, docClassAssignment = null;
				int docID = 0;

				Map<String, Float> currDocIdftfMap = new HashMap<String, Float>();

				docName = currInstance.toString(0);
				docID = Integer.parseInt(currInstance.toString(1));
				docClassAssignment = currInstance.toString(data.classIndex());

				Document currDoc = new Document(docName, docID,
						docClassAssignment);

				//hard coded positions of the attributes ( from 2 to numAttributes-1 )
				for (int j = 2; j < currInstance.numAttributes() - 1; j++) {

					if (!currInstance.toString(j).equals("0")) {
						currDocIdftfMap.put(currInstance.attribute(j).name(),
								Float.parseFloat(currInstance.toString(j)));

						// System.out.println("attr: "
						// + currInstance.attribute(j).name() + " toString: "
						// + currInstance.toString(j));
					}

				}

				documentVectors
						.add(new DocumentVector(currDoc, currDocIdftfMap));
				// System.out.println("Document " +docClassAssignment + docName
				// + " added");
			}

			System.out.println("building DocumentVectors finished");
			return documentVectors;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
