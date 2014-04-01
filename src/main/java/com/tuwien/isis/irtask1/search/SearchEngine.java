package com.tuwien.isis.irtask1.search;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class SearchEngine {
	private List<DocumentVector> documentVectors;
	private int numberOfSearchResults;

	public SearchEngine(int numberOfSearchResults) {
		this.numberOfSearchResults = numberOfSearchResults;
	}

	public void searchSimilarDocuments(String inputFilePath,
			String indexFilePath) {

		documentVectors = DocumentVectorBuilder
				.buildDocumentVectors(indexFilePath);

		processSimilarityRetrieval(inputFilePath);
	}

	/**
	 * @return the numberOfSearchResults
	 */
	public int getNumberOfSearchResults() {
		return numberOfSearchResults;
	}

	/**
	 * @param numberOfSearchResults
	 *            the numberOfSearchResults to set
	 */
	public void setNumberOfSearchResults(int numberOfSearchResults) {
		this.numberOfSearchResults = numberOfSearchResults;
	}

	private void processSimilarityRetrieval(String inputFilePath) {
		System.out.println("Reading topic file - " + inputFilePath);
		Path path = Paths.get(inputFilePath);
		try {
			Scanner scanner = new Scanner(path);

			int topicNr = 1;

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();

				DocumentVector currQueryDocVec = findQueryInDocumentList(line);

				List<DocumentVector> resultList = CosineSimilarityRetrieval
						.buildSearchResultsList(currQueryDocVec,
								documentVectors);

				Collections.sort(resultList,
						new DocumentVectorCosineComparator());

				// for debug
				// for (int j = 0; j < resultList.size(); j++) {
				// System.out.println("- "
				// + resultList.get(j).getFullDocName() + " - cos: "
				// + resultList.get(j).getCosine());
				// }

				String outputFilePath = "output/" + "topic_" + topicNr
						+ "_results" + ".txt";

				writeSearchResultsIntoFile(outputFilePath, resultList, topicNr);

				System.out.println("finished processing query: " + line);

				topicNr++;
			}

			scanner.close();

		} catch (IOException e) {
			System.err.println("error with InputFile Scanner: "
					+ e.getMessage());

			// e.printStackTrace();
		}
	}

	/**
	 * parses the parameter String line to a DocumentVector
	 * 
	 * 
	 * @param line
	 * @return
	 */
	private DocumentVector findQueryInDocumentList(String line) {
		DocumentVector currDocVec = null;

		// search document in document list
		for (int i = 0; i < documentVectors.size(); i++) {
			// String name = documentVectors.get(i).
			// System.out.println(documentVectors.get(i).getFullDocName());
			if (line.equals(documentVectors.get(i).getFullDocName())) {
				currDocVec = documentVectors.get(i);
			}
		}

		if (currDocVec == null) {
			System.out.println("Document " + line + " not in collection");

		}
		return currDocVec;
	}

	private void writeSearchResultsIntoFile(String outputFilePath,
			List<DocumentVector> resultList, int topicNr) {
		try {
			File file = new File(outputFilePath);
			file.getParentFile().mkdirs();
			PrintWriter writer = new PrintWriter(file);

			if (this.numberOfSearchResults > resultList.size())
				this.numberOfSearchResults = resultList.size();

			for (int k = 0; k < this.numberOfSearchResults; k++) {
				DocumentVector currVec = resultList.get(k);
				// TODO
				// topic1 Q0 misc.forsale/74721 1 34.32 group10_medium
				String line = "topic" + topicNr + " Q0 "
						+ currVec.getFullDocName()
						+ " "
						// + currVec.getDocumentData().m_docID
						+ k + " "
						+ String.format("%.2f", currVec.getCosine() * 100)
						+ " group3" + "_";// +
											// getCurrentPostingListSizeString();

				writer.write(line);
				writer.write("\n");
				System.out.println("Writing: " + line);
			}

			writer.close();

		} catch (FileNotFoundException e) {
			System.err.println("Couldn'T write into file: " + outputFilePath
					+ " error: " + e.getMessage());
			// e.printStackTrace();
		}
	}

}
