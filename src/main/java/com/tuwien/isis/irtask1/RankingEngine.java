package com.tuwien.isis.irtask1;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * TODO
 */
public class RankingEngine {

	/**
	 * CLI option for setting the input topics list
	 */
	private static final String TOPICS = "t";

	/**
	 * Handle user arguments
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		// Create command line parser
		Options options = new Options();
		options.addOption(TOPICS, true, "list of input topics");
		CommandLineParser parser = new PosixParser();

		try {
			CommandLine command = parser.parse(options, args);
			if (command.hasOption(TOPICS)) {
				System.out.println(command.getOptionValue(TOPICS));
			} else {
				throw new MissingOptionException("Topic list was not specified. Please use the -" + TOPICS + " option");
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
}
