package indexer;

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

public class Indexer {

	private boolean useStemmer;
	private int m_minimumFrequencyThreshold;
	private int m_maximumFrequencyThreshold;

	private Set<String> m_classAssignmentSet;
	private Set<String> m_tokenSet;
	private ArrayList<String> m_sortedTokenList;

	private ArrayList<DocumentData> m_documentsList;
	private Map<String, ArrayList<Posting>> m_dictionary;

	private Map<String, Set<Integer>> m_tokenDocumentFrequency;
	private Map<String, Map<Integer, Float>> m_tokenIdfTfMap;

	private int m_currentDocumentID;
	private String m_currentDocClassName;

	private Stemmer stemmer;

	public Indexer(IndexerParameters indexParameters) {
		useStemmer = indexParameters.isStemmingOn;
		m_minimumFrequencyThreshold = indexParameters.minFreqThreshold;
		if (indexParameters.maxFreqThreshold <= 0) {
			m_maximumFrequencyThreshold = 2147483647;
		} else {
			m_maximumFrequencyThreshold = indexParameters.maxFreqThreshold;
		}

		m_currentDocumentID = 0;
		m_currentDocClassName = "";

		m_classAssignmentSet = new HashSet<String>();
		m_tokenSet = new HashSet<String>();
		m_sortedTokenList = new ArrayList<String>();

		m_tokenIdfTfMap = new HashMap<String, Map<Integer, Float>>();

		m_documentsList = new ArrayList<DocumentData>();
		m_dictionary = new HashMap<String, ArrayList<Posting>>();

		stemmer = new Stemmer();
	}

	public void readInAllDocuments(String path) throws IOException {
		File root = new File(path);
		File[] list = root.listFiles();

		for (File f : list) {
			if (f.isDirectory()) {
				System.out.println("Indexing files in dir: " + f.getAbsoluteFile());
				String directoryPath = f.getAbsoluteFile().getAbsolutePath();
				int index = directoryPath.lastIndexOf('\\');
				m_currentDocClassName = directoryPath.substring(index + 1);

				readInAllDocuments(f.getAbsolutePath());
			} else {
				indexFile(f);
			}
		}
	}

	private void calculateTFIDF() {
		int docCount = m_documentsList.size();

		Iterator<Entry<String, Set<Integer>>> tokenDocFreqIter = m_tokenDocumentFrequency.entrySet().iterator();
		// For every token (get the counted document frequency of each token)
		while (tokenDocFreqIter.hasNext()) {
			Entry<String, Set<Integer>> currentPair = tokenDocFreqIter.next();
			String currentToken = currentPair.getKey();
			ArrayList<Posting> postings = m_dictionary.get(currentToken);

			int documentFrequency = currentPair.getValue().size();

			calculateIDFTFForToken(docCount, currentToken, postings, documentFrequency);
		}
	}

	private void calculateIDFTFForToken(int docCount, String currentToken, ArrayList<Posting> postings,
			int documentFrequency) {
		Map<Integer, Float> currentDocIDFTFMap = null;
		int j = 0;

		// For every document in our list
		int documentsCount = m_documentsList.size();
		for (int i = 0; i < documentsCount; ++i) {
			int currentDocumentID = m_documentsList.get(i).getDocumentId();

			int postingsCount = postings.size();

			// Get token occurances in current document
			int tokenOccurancesInDocument = 0;
			for (; j < postingsCount; ++j) {
				int curDocID = postings.get(j).getDocumentId();
				if (curDocID > currentDocumentID)
					break;
				else if (curDocID == currentDocumentID) {
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
					m_tokenIdfTfMap.put(currentToken, currentDocIDFTFMap);
				}

				currentDocIDFTFMap.put(currentDocumentID, tfIdfValue);
			}
		}
	}

	private void indexFile(File f) throws IOException {
		String documentPath = f.getAbsoluteFile().getPath();
		String documentString = readWholeDocument(documentPath);
		String fileName = f.getName();

		DocumentData data = new DocumentData(fileName, m_currentDocumentID, m_currentDocClassName);
		m_documentsList.add(data);
		m_classAssignmentSet.add(m_currentDocClassName);

		String documentContent[] = documentString.split("\\W");

		for (String currentWordString : documentContent) {
			processWord(currentWordString);
		}

		++m_currentDocumentID;
	}

	private void processWord(String currentWordString) {
		if (currentWordString.matches("[a-zA-Z]+")) {
			if (useStemmer) {
				// TODO fix currentWordString = stemmer.stemToken(currentWordString);

				if (currentWordString.length() >= 2)
					addToken(currentWordString.toLowerCase());
			} else
				addToken(currentWordString);
		}
	}

	private void addToken(String token) {
		addToDictionary(token, m_currentDocumentID);

		Set<Integer> docFreqSet = m_tokenDocumentFrequency.get(token);
		if (docFreqSet == null) {
			docFreqSet = new HashSet<Integer>();
			docFreqSet.add(m_currentDocumentID);
			m_tokenDocumentFrequency.put(token, docFreqSet);
		} else {
			docFreqSet.add(m_currentDocumentID);
		}
	}

	private void addToDictionary(String token, int m_currentDocumentID) {
		Posting newPosting = new Posting(m_currentDocumentID, 0);

		ArrayList<Posting> tokenPosting = m_dictionary.get(token);
		if (tokenPosting == null) {
			tokenPosting = new ArrayList<Posting>();
			tokenPosting.add(newPosting);
			m_dictionary.put(token, tokenPosting);

			m_tokenSet.add(token);
		} else {
			tokenPosting.add(newPosting);
		}
	}

	private String readWholeDocument(String path) throws IOException {
		FileInputStream stream = new FileInputStream(new File(path));
		try {
			FileChannel fc = stream.getChannel();
			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());

			return Charset.defaultCharset().decode(bb).toString();
		} finally {
			stream.close();
		}
	}

	public Map<String, ArrayList<Posting>> getDictionary() {
		return m_dictionary;
	}

	public ArrayList<DocumentData> getDocumentsList() {
		return m_documentsList;
	}

	public Set<String> getClassAssignmentSet() {
		return m_classAssignmentSet;
	}

	public ArrayList<String> getSortedTokenList() {
		return m_sortedTokenList;
	}

	public Map<String, Map<Integer, Float>> getTokenIdfTfMap() {
		return m_tokenIdfTfMap;
	}

	// Once done with the indexing we can calculate the tf-idf etc.
	public void processIndexing() {
		calculateTFIDF();
	}

	public void prepareIndexing() {
		m_tokenDocumentFrequency = new HashMap<String, Set<Integer>>();
	}

	public void removeTokensDependantOnThreshold() {
		Iterator<Entry<String, Set<Integer>>> tokenDocFreqIter = m_tokenDocumentFrequency.entrySet().iterator();
		// For every token (get the counted document frequency of each token)
		while (tokenDocFreqIter.hasNext()) {
			Entry<String, Set<Integer>> currentPair = tokenDocFreqIter.next();
			String currentToken = currentPair.getKey();
			Set<Integer> currentSet = currentPair.getValue();
			int frequencyCount = currentSet.size();

			if (frequencyCount > m_maximumFrequencyThreshold || frequencyCount < m_minimumFrequencyThreshold) {
				m_tokenSet.remove(currentToken);
				m_dictionary.remove(currentToken);
				m_tokenIdfTfMap.remove(currentToken);

				tokenDocFreqIter.remove();
			}
		}
	}

	public void createSortedTokenList() {
		Iterator<String> it = m_tokenSet.iterator();

		while (it.hasNext()) {
			String value = it.next();
			m_sortedTokenList.add(value);
		}

		Collections.sort(m_sortedTokenList);

		m_tokenSet = null;
	}
}
