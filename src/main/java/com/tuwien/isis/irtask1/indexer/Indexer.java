package com.tuwien.isis.irtask1.indexer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.tartarus.martin.Stemmer;

import weka.core.Stopwords;

/**
 * Create an index on the document collection
 * 
 * @author Taylor
 */
public class Indexer {

	/**
	 * Default value to use for the maximum frequency threshold if no other value is given
	 */
	private static final int DEFAULT_MAX_FREQ_THRESHOLD = 2147483647;

	/**
	 * Minimum length in characters for a term to be indexed
	 */
	private static final int MIN_TERM_LENGTH = 2;

	/**
	 * Flag indicating if stemming should be used during the creation of the index
	 */
	private boolean useStemmer;

	/**
	 * Flag indicating if stopwords should be removed during the creation of the index
	 */
	private boolean removeStopwords;

	private int minFreqThreshold;
	private int maxFreqThreshold;

	/**
	 * Flag indicating if the index has been generated yet
	 */
	private boolean indexGenerated = false;

	private Set<String> classAssignmentSet = new HashSet<String>();
	private Set<String> tokenSet = new HashSet<String>();
	private ArrayList<String> sortedTokenList = new ArrayList<String>();

	private ArrayList<Document> documentsList = new ArrayList<Document>();

	/**
	 * Data structure to record in which documents terms occur
	 */
	private Map<String, ArrayList<Posting>> dictionary = new HashMap<String, ArrayList<Posting>>();

	private Map<String, Set<Integer>> tokenDocumentFrequency = new HashMap<String, Set<Integer>>();
	private Map<String, Map<Integer, Float>> tokenIdfTfMap = new HashMap<String, Map<Integer, Float>>();

	private int currentDocumentId = 0;
	private String currentDocClassName;

	private Stemmer stemmer = new Stemmer();

	/**
	 * Initialize the indexer
	 * 
	 * @param useStemmer
	 * @param minFreqThreshold
	 * @param maxFreqThreshold
	 */
	public Indexer(boolean useStemmer, boolean removeStopwords, int minFreqThreshold, int maxFreqThreshold) {
		this.useStemmer = useStemmer;
		this.removeStopwords = removeStopwords;
		this.minFreqThreshold = minFreqThreshold;
		this.maxFreqThreshold = (maxFreqThreshold <= 0) ? DEFAULT_MAX_FREQ_THRESHOLD : maxFreqThreshold;
	}

	/**
	 * Create an index as an ARFF file and store it to disk
	 * 
	 * @throws IOException
	 * 
	 * @throws Exception
	 */
	public void createIndex(String path) throws IOException {
		if (!indexGenerated) {
			readDocumentCollection(path);
			removeTokensDependantOnThreshold();
			calculateTfidf();
			createSortedTokenList();
			indexGenerated = true;
		} else {
			System.err.println("Index has already been generated.");
		}
	}

	/**
	 * Store the index to disk (if it has been generated)
	 * 
	 * @throws Exception
	 */
	public void storeIndex() throws Exception {
		if (indexGenerated) {
			System.out.println("Starting to write index to file");
			ARFFWriter.writeIndexToFile(this);
			System.out.println("Indexing and writing finished.");
		} else {
			System.err.println("Index has not yet been generated.");
		}
	}

	/**
	 * Add all files in the given directory
	 * 
	 * @param path
	 * @throws IOException
	 */
	private void readDocumentCollection(String path) throws IOException {
		File root = new File(path);
		File[] list = root.listFiles();

		for (File file : list) {
			if (file.isDirectory()) {
				System.out.println("Indexing files in directory: " + file.getAbsoluteFile());
				String directoryPath = file.getAbsoluteFile().getAbsolutePath();
				currentDocClassName = generateCurrentDocClassName(directoryPath);
				readDocumentCollection(file.getAbsolutePath());
			} else {
				indexFile(file);
			}
		}
	}

	/**
	 * Use the name of a file's parent folder to generate a class name for the current document
	 * 
	 * @param directoryPath
	 * @return
	 */
	private String generateCurrentDocClassName(String directoryPath) {
		int index = directoryPath.lastIndexOf('\\');
		return directoryPath.substring(index + 1);
	}

	/**
	 * Process each document in the collection to be indexed
	 * 
	 * @param file
	 * @throws IOException
	 */
	private void indexFile(File file) throws IOException {
		String documentString = readDocumentContents(file);
		String fileName = file.getName();

		Document data = new Document(fileName, currentDocumentId, currentDocClassName);
		documentsList.add(data);
		classAssignmentSet.add(currentDocClassName);

		// Split the document string on all non-word characters
		String documentContent[] = documentString.split("\\W");

		for (String currentWordString : documentContent) {
			processToken(currentWordString.toLowerCase());
		}

		currentDocumentId++;
	}

	/**
	 * Return the contents of a file with a given path in the form of a string
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	private String readDocumentContents(File file) throws IOException {
		FileInputStream stream = new FileInputStream(file);
		try {
			FileChannel fc = stream.getChannel();
			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
			return Charset.defaultCharset().decode(bb).toString();
		} finally {
			stream.close();
		}
	}

	/**
	 * Add a given token to the tokens collection if it meets the processing criteria
	 * 
	 * @param token
	 */
	private void processToken(String token) {
		if (token.matches("[a-zA-Z]+") && token.length() >= MIN_TERM_LENGTH) {

			// Check for stopwords (if enabled)
			if (removeStopwords && Stopwords.isStopword(token)) {
				return;
			}

			// Apply stemming (if enabled)
			if (useStemmer) {
				char[] wordChar = token.toCharArray();
				stemmer.add(wordChar, wordChar.length);
				stemmer.stem();
				token = String.valueOf(stemmer.getResultBuffer(), 0, stemmer.getResultLength());
			}

			addToken(token);
		}
	}

	/**
	 * Record an occurrence of a token in the current document
	 * 
	 * @param token
	 */
	private void addToken(String token) {
		addToDictionary(token, currentDocumentId);

		// Check if a frequency entry already exists for this token
		Set<Integer> tokenFrequency = tokenDocumentFrequency.get(token);
		if (tokenFrequency == null) {
			tokenFrequency = new HashSet<Integer>();
			tokenDocumentFrequency.put(token, tokenFrequency);
		}

		// Record the occurrence of the current token in the current document
		tokenFrequency.add(currentDocumentId);
	}

	/**
	 * Record an occurrence of a term in the dictionary
	 * 
	 * @param token
	 * @param currentDocumentId
	 */
	private void addToDictionary(String token, int currentDocumentId) {
		Posting posting = new Posting(currentDocumentId, 0);

		// Check if a posting for this token already exists
		ArrayList<Posting> tokenPosting = dictionary.get(token);
		if (tokenPosting == null) {
			tokenPosting = new ArrayList<Posting>();
			tokenPosting.add(posting);
			dictionary.put(token, tokenPosting);
			tokenSet.add(token);
		}

		// Record the posting for this token
		tokenPosting.add(posting);
	}

	/**
	 * Calculate the term frequency–inverse document frequency for all tokens
	 */
	private void calculateTfidf() {
		int docCount = documentsList.size();
		for (Entry<String, Set<Integer>> entry : tokenDocumentFrequency.entrySet()) {
			String token = entry.getKey();
			int documentFrequency = entry.getValue().size();
			ArrayList<Posting> postings = dictionary.get(token);
			calculateIDFTFForToken(docCount, token, postings, documentFrequency);
		}
	}

	/**
	 * TODO
	 * 
	 * @param docCount
	 * @param currentToken
	 * @param postings
	 * @param documentFrequency
	 */
	private void calculateIDFTFForToken(int docCount, String currentToken, ArrayList<Posting> postings,
			int documentFrequency) {
		Map<Integer, Float> currentDocIDFTFMap = null;
		int j = 0;

		// For every document in our list
		int documentsCount = documentsList.size();
		for (int i = 0; i < documentsCount; ++i) {
			int currentDocumentId = documentsList.get(i).getId();

			int postingsCount = postings.size();

			// Get token occurrences in current document
			int tokenOccurancesInDocument = 0;
			for (; j < postingsCount; ++j) {
				int curDocID = postings.get(j).getId();
				if (curDocID > currentDocumentId) {
					break;
				} else if (curDocID == currentDocumentId) {
					++tokenOccurancesInDocument;
				}
			}

			float idfValue = (float) Math.log10(docCount / (float) documentFrequency);

			float tfValue = 0;
			if (tokenOccurancesInDocument != 0)
				tfValue = (float) (1.0 + Math.log10(tokenOccurancesInDocument));

			float tfIdfValue = idfValue * tfValue;

			if (tfIdfValue > 0) {
				// If needed make new map
				if (currentDocIDFTFMap == null) {
					currentDocIDFTFMap = new HashMap<Integer, Float>();
					tokenIdfTfMap.put(currentToken, currentDocIDFTFMap);
				}

				currentDocIDFTFMap.put(currentDocumentId, tfIdfValue);
			}
		}
	}

	/**
	 * TODO
	 */
	private void removeTokensDependantOnThreshold() {
		Iterator<Entry<String, Set<Integer>>> tokenDocFreqIter = tokenDocumentFrequency.entrySet().iterator();
		// For every token (get the counted document frequency of each token)
		while (tokenDocFreqIter.hasNext()) {
			Entry<String, Set<Integer>> currentPair = tokenDocFreqIter.next();
			String currentToken = currentPair.getKey();
			Set<Integer> currentSet = currentPair.getValue();
			int frequencyCount = currentSet.size();

			if (frequencyCount > maxFreqThreshold || frequencyCount < minFreqThreshold) {
				tokenSet.remove(currentToken);
				dictionary.remove(currentToken);
				tokenIdfTfMap.remove(currentToken);
				tokenDocFreqIter.remove();
			}
		}
	}

	/**
	 * TODO
	 */
	private void createSortedTokenList() {
		for (String token : tokenSet) {
			sortedTokenList.add(token);
		}

		Collections.sort(sortedTokenList);

		tokenSet = null;
	}

	public Map<String, ArrayList<Posting>> getDictionary() {
		return dictionary;
	}

	public ArrayList<Document> getDocumentsList() {
		return documentsList;
	}

	public Set<String> getClassAssignmentSet() {
		return classAssignmentSet;
	}

	public ArrayList<String> getSortedTokenList() {
		return sortedTokenList;
	}

	public Map<String, Map<Integer, Float>> getTokenIdfTfMap() {
		return tokenIdfTfMap;
	}
}
