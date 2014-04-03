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

/**
 * SearchEngine which performs the search
 * (currently with only VSM-Cosine similarity method)
 * @author Wagi
 *
 */
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
		System.out.println("Processing topic file - " + inputFilePath);
		Path path = Paths.get(inputFilePath);
		try {
			Scanner scanner = new Scanner(path);

			int topicNr = 1;

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				
				System.out.println("query: " +line);

				DocumentVector currQueryDocVec = findQueryInDocumentList(line);	
				if(currQueryDocVec==null){
					System.out.println("skipping input query " + line);
					
					topicNr++;
					continue;
				}
				
				System.out.println(currQueryDocVec);
				
				List<DocumentVector> resultList = CosineSimilarityRetrieval
						.buildSearchResultsList(currQueryDocVec,
								documentVectors);

				//sort result list
				Collections.sort(resultList,
						new DocumentVectorCosineComparator());

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
		// search document in document list	
		for(DocumentVector currDocVec: documentVectors){
			
			//System.out.println("currDocVec fullname= " + currDocVec.getFullDocName()); 
			if(line.equals(currDocVec.getFullDocName())){
				return currDocVec;
			}
		}		
		//in case nothing found return null
		System.out.println("doc " + line + " not in collection");
		return null;		
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
				// TODO add run_name - depending on search parameters/index
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
