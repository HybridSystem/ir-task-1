package com.tuwien.isis.irtask1.indexer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.tartarus.martin.Stemmer;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Stopwords;
import weka.core.converters.ArffSaver;
import weka.core.converters.Saver;

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

	/**
	 * The minimum number of occurrences a word must have in the collection to be indexed
	 */
	private int minFreqThreshold;

	/**
	 * The maximum number of occurrences a word may have in the collection to be indexed
	 */
	private int maxFreqThreshold;

	/**
	 * Flag indicating if the index has been generated yet
	 */
	private boolean indexGenerated = false;

	/**
	 * Set of all class assignments
	 */
	private Set<String> classAssignmentSet = new HashSet<String>();

	/**
	 * Set of all tokens
	 */
	private Set<String> tokenSet = new HashSet<String>();

	/**
	 * List of documents in the collections
	 */
	private List<Document> documentList = new ArrayList<Document>();

	/**
	 * Data structure to record in which documents terms occur
	 */
	private Map<String, ArrayList<Posting>> dictionary = new HashMap<String, ArrayList<Posting>>();

	/**
	 * Map of a token's frequency in documents
	 */
	private Map<String, Set<Integer>> tokenDocumentFrequency = new HashMap<String, Set<Integer>>();

	/**
	 * Map of the IDFTF score of a token
	 */
	private Map<String, Map<Integer, Float>> tokenIdftfMap = new HashMap<String, Map<Integer, Float>>();

	/**
	 * Internal counter used to mark the document currently being processed
	 */
	private int currentDocumentId = 0;

	/**
	 * Internal counter used to mark the document class currently being processed
	 */
	private String currentDocClassName;

	/**
	 * Stemming object to transform words into their root form
	 */
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
			applyTokenThresholds();
			calculateTfidf();
			indexGenerated = true;
		} else {
			System.err.println("Index has already been generated.");
		}
	}

	/**
	 * Store the index to disk (if it has been generated)
	 * 
	 * @param path
	 * @throws Exception
	 */
	public void storeIndex(String path) throws IOException {
		if (indexGenerated) {
			System.out.println("Writing index file to disk...");
			writeIndexToFile(path);
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
		documentList.add(data);
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

		// Check if the token is made up of word characters and meets minimum length requirements
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
	 * Remove tokens if their frequency does not fall within the user-defined range
	 */
	private void applyTokenThresholds() {

		// Set of tokens to be removed
		Set<String> toRemove = new HashSet<String>();

		// Check all tokens
		for (Entry<String, Set<Integer>> entry : tokenDocumentFrequency.entrySet()) {
			String token = entry.getKey();
			Set<Integer> currentSet = entry.getValue();
			int frequencyCount = currentSet.size();

			if (frequencyCount > maxFreqThreshold || frequencyCount < minFreqThreshold) {
				tokenSet.remove(token);
				dictionary.remove(token);
				tokenIdftfMap.remove(token);
				toRemove.add(token);
			}
		}

		// Remove tokens whose frequency was outside of the defined threshold
		tokenDocumentFrequency.keySet().removeAll(toRemove);
	}

	/**
	 * Calculate the term frequency–inverse document frequency for all tokens
	 */
	private void calculateTfidf() {
		for (Entry<String, Set<Integer>> entry : tokenDocumentFrequency.entrySet()) {
			String token = entry.getKey();
			int documentFrequency = entry.getValue().size();
			ArrayList<Posting> postings = dictionary.get(token);
			calculateTfidfForToken(token, postings, documentFrequency);
		}
	}

	/**
	 * Calculate the term frequency–inverse document frequency for a given token
	 * 
	 * @param token
	 * @param postings
	 * @param documentFrequency
	 */
	private void calculateTfidfForToken(String token, ArrayList<Posting> postings, int documentFrequency) {

		Map<Integer, Float> currentDocIdftfMap = null;

		for (Document document : documentList) {
			int documentId = document.getId();
			int occurancesInDocument = 0;
			for (Posting posting : postings) {
				int curDocID = posting.getId();
				if (curDocID > documentId) {
					break;
				} else if (curDocID == documentId) {
					occurancesInDocument++;
				}
			}

			// Calculate term frequency–inverse document frequency
			float idfValue = (float) Math.log10(documentList.size() / (float) documentFrequency);
			float tfValue = (occurancesInDocument != 0) ? (float) (1.0 + Math.log10(occurancesInDocument)) : 0;
			float tfidfValue = idfValue * tfValue;

			if (tfidfValue > 0) {

				// Round the value to a reasonable amount of precision
				DecimalFormat formatter = new DecimalFormat("#.###");
				tfidfValue = Float.parseFloat(formatter.format(tfidfValue));

				// Check if map needs to be created
				if (currentDocIdftfMap == null) {
					currentDocIdftfMap = new HashMap<Integer, Float>();
					tokenIdftfMap.put(token, currentDocIdftfMap);
				}

				currentDocIdftfMap.put(documentId, tfidfValue);
			}
		}
	}

	/**
	 * Write the generated index file to disk
	 * 
	 * @param path
	 * @throws IOException
	 */
	public void writeIndexToFile(String path) throws IOException {

		// Sort token set and convert to list
		List<String> sortedTokenList = new ArrayList<String>(tokenSet);
		Collections.sort(sortedTokenList);

		FastVector attributes = new FastVector();
		attributes.addElement(new Attribute("filename", (FastVector) null));
		attributes.addElement(new Attribute("document-id"));
		FastVector classAssignmentValues = addAttributeClassAssignment(classAssignmentSet, attributes);
		addAttributesTokens(sortedTokenList, attributes);

		Instances instances = new Instances("Index", attributes, 0);
		addDocumentData(instances, classAssignmentValues, sortedTokenList.size());

		ArffSaver saver = new ArffSaver();
		saver.setFile(new File(path));
		saver.setRetrieval(Saver.INCREMENTAL);
		saver.setStructure(instances);

		// Write each instance to disk incrementally
		for (int instanceIndex = 0; instanceIndex < instances.numInstances(); instanceIndex++) {
			saver.writeIncremental(instances.instance(instanceIndex));
		}
	}

	/**
	 * Add the document data to the instances
	 * 
	 * @param documentsList
	 * @param instances
	 * @param classAssignmentValues
	 * @param tokenIdftfMap
	 * @param tokenAttributesCount
	 */
	private void addDocumentData(Instances instances, FastVector classAssignmentValues, int tokenAttributesCount) {

		for (Document document : documentList) {
			double[] dataValues = new double[instances.numAttributes()];

			dataValues[0] = instances.attribute(0).addStringValue(document.getName());
			dataValues[1] = document.getId();
			dataValues[2] = classAssignmentValues.indexOf(document.getClassAssignment());

			for (int index = 0; index < tokenAttributesCount; ++index) {

				int attributeIndex = index + 3;

				// Get IDFTF map of the current token
				Map<Integer, Float> docIdftfMap = tokenIdftfMap.get(instances.attribute(attributeIndex).name());

				if (docIdftfMap != null && docIdftfMap.get(document.getId()) != null) {
					dataValues[attributeIndex] = docIdftfMap.get(document.getId());
				} else {
					dataValues[attributeIndex] = 0;
				}
			}
			
			instances.add(new Instance(1.0, dataValues));
		}
	}

	/**
	 * Add the class assignments as attributes
	 * 
	 * @param classAssignmentSet
	 * @param attributes
	 * @return
	 */
	private static FastVector addAttributeClassAssignment(Set<String> classAssignmentSet, FastVector attributes) {

		FastVector classAssignmentValues = new FastVector();
		for (String classAssignment : classAssignmentSet) {
			classAssignmentValues.addElement(classAssignment);
		}

		attributes.addElement(new Attribute("class-assignment", classAssignmentValues));
		return classAssignmentValues;
	}

	/**
	 * Adds the tokens as attributes
	 * 
	 * @param sortedTokenList
	 * @param attributes
	 * @return
	 */
	private static void addAttributesTokens(List<String> sortedTokenList, FastVector attributes) {
		for (String token : sortedTokenList) {
			attributes.addElement(new Attribute(token));
		}
	}
}
