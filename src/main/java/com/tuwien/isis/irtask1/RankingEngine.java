package com.tuwien.isis.irtask1;

import com.tuwien.isis.irtask1.indexer.Indexer;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * TODO
 */
public class RankingEngine {

	/**
	 * CLI option for running the indexer
	 */
	private static final String INDEXER = "i";

	/**
	 * CLI option for setting the input topics list
	 */
	private static final String TOPICS = "t";

	/**
	 * CLI option for enabling stemming
	 */
	private static final String STEMMING = "stem";

	/**
	 * CLI option for enabling removing stopwords
	 */
	private static final String STOPWORDS = "stop";

	/**
	 * CLI option for setting minimum number of term occurrences
	 */
	private static final String MIN_FREQ = "min";

	/**
	 * CLI option for setting maximum number of term occurrences
	 */
	private static final String MAX_FREQ = "max";

	/**
	 * Path to the document collection
	 */
	private static final String COLLECTION_PATH = "collection";

	/**
	 * Handle user arguments
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		// Create user options and command line parser
		Options options = new Options();
		options.addOption(INDEXER, false, "run indexer");
		options.addOption(STEMMING, false, "use stemming during the creation of the index");
		options.addOption(STOPWORDS, false, "remove stopwords during the creation of the index");
		options.addOption(TOPICS, false, "list of input topics");
		Option min = new Option(MIN_FREQ, true, "minimum term frequency required to be added to index");
		Option max = new Option(MAX_FREQ, true, "maximum term frequency allowed to be included in index");
		options.addOption(min);
		options.addOption(max);
		CommandLineParser parser = new PosixParser();

		try {
			CommandLine command = parser.parse(options, args);

			if (command.hasOption(INDEXER)) {

				// Retrieve user options to use for index creation
				boolean useStemming = command.hasOption(STEMMING);
				boolean removeStopwords = command.hasOption(STOPWORDS);
				int minFreq = command.hasOption(MIN_FREQ) ? Integer.parseInt(command.getOptionValue(MIN_FREQ)) : 0;
				int maxFreq = command.hasOption(MAX_FREQ) ? Integer.parseInt(command.getOptionValue(MAX_FREQ)) : 0;

				// Create and run indexer
				Indexer indexer = new Indexer(useStemming, removeStopwords, minFreq, maxFreq);
				indexer.createIndex(COLLECTION_PATH);
				indexer.storeIndex();
			} else if (false) {
				// TODO
				// String topicList = getTopicList(command);
			} else {
				System.out.println("no option selected");
			}

		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * TODO
	 * 
	 * @param command
	 * @return
	 * @throws MissingOptionException
	 */
	private static String getTopicList(CommandLine command) throws MissingOptionException {

		if (command.hasOption(TOPICS)) {
			return command.getOptionValue(TOPICS);
		} else {
			throw new MissingOptionException("Topic list was not specified. Please use the -" + TOPICS + " option");
		}

	}
}
