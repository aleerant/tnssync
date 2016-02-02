package com.aleerant.tnssync;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class PropertiesHandler implements APPCONSTANT {

	private CommandLineParser mParser = new DefaultParser();
	private Options mOptions = new Options();
	private String mTnsAdminPathString, mDefaultAdminContext, mDirectoryServers;

	public PropertiesHandler(String[] args) throws AppException {
		initOptions();

		try {
			CommandLine cl = mParser.parse(mOptions, args);

			if (cl.hasOption("h")) {
				printUsageInfo();
				System.exit(0);
			}

			if (cl.hasOption("v")) {
				printVersion();
				System.exit(0);
			}

			if (cl.hasOption("ta")) {
				mTnsAdminPathString = cl.getOptionValue("ta");
			} else {
				mTnsAdminPathString = (System.getenv()).get("TNS_ADMIN");
				if (mTnsAdminPathString == null) {
					throw new IllegalArgumentException("Missing envinronment variable: TNS_ADMIN");
				}
			}

		} catch (ParseException e) {
			// oops, something went wrong
			System.err.println("parsing failed.  Reason: " + e.getMessage());
			printUsageInfo();
			System.exit(1);
		}

		selectLoggingConfigFile();
		validateTnsAdminPath();
		getLdapOraProperties();
	}

	private void initOptions() {
		Option helpOption = Option.builder("h").longOpt("help").desc("Print this message").build();
		Option versionOption = Option.builder("v").longOpt("version").desc("Print the version of the application")
				.build();
		Option tnsAdminPathOption = Option.builder("ta").longOpt("tns_admin_dir").argName("DIR").hasArg()
				.desc("Specifies a directory where the SQL*Net configuration files (like sqlnet.ora, ldap.ora and tnsnames.ora) are located."
						+ " Configuration file for this program (" + APP_TNSSYNC_FILENAME + ") is also found here.")
				.build();
		mOptions.addOption(helpOption);
		mOptions.addOption(versionOption);
		mOptions.addOption(tnsAdminPathOption);
	}

	private void validateTnsAdminPath() {
		if (this.mTnsAdminPathString == null) {
			throw new IllegalArgumentException("Missing parameter: TNS_ADMIN");
		}
		Path p = Paths.get(this.mTnsAdminPathString);
		if (Files.notExists(p)) {
			throw new IllegalArgumentException("Directory not found (TNS_ADMIN): " + p.toString());
		}
	}

	private void getLdapOraProperties() throws AppException {
		String ldaporaFile = Paths.get(mTnsAdminPathString, APP_LDAPORA_FILENAME).toString();
		Properties ldapOraProperties = new Properties();
		try {
			ldapOraProperties.load(new FileInputStream(ldaporaFile));
		} catch (FileNotFoundException e) {
			throw new AppException(APP_LDAPORA_FILENAME + " file is missing (" + ldaporaFile + "), error message: "
					+ e.getMessage() + ", caused by:" + e.getCause());
		} catch (IOException e) {
			throw new AppException("can not read " + APP_LDAPORA_FILENAME + " file (" + ldaporaFile
					+ "), error message: " + e.getMessage() + ", caused by:" + e.getCause());
		}

		this.mDefaultAdminContext = ldapOraProperties.getProperty("DEFAULT_ADMIN_CONTEXT").replaceAll("[()\\s]", "");
		if (this.mDefaultAdminContext == null) {
			throw new IllegalArgumentException("Missing parameter: DEFAULT_ADMIN_CONTEXT");
		}
		this.mDirectoryServers = ldapOraProperties.getProperty("DIRECTORY_SERVERS", "").replaceAll("[()\\s]", "");
	}

	private void selectLoggingConfigFile() {
		if (Files.notExists(Paths.get(mTnsAdminPathString, APP_TNSSYNC_LOGBACK_FILENAME))) {
			return;
		}

		LoggerContext context = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
		try {
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);
			context.reset();
			configurator.doConfigure(Paths.get(mTnsAdminPathString, APP_TNSSYNC_LOGBACK_FILENAME).toString());
		} catch (JoranException e) {
			// StatusPrinter will handle this
		}
		StatusPrinter.printInCaseOfErrorsOrWarnings(context);
	}

	private void printUsageInfo() {
		final String header = "\n" + APP_NAME + " creates " + APP_TNSNAMES_FILENAME
				+ " file for services listed in a config file (" + APP_TNSSYNC_FILENAME
				+ ") getting Oracle Net Description data from a directory server (described in " + APP_LDAPORA_FILENAME
				+ ").\n\n";
		final String footer = "\n"
				+ "Licensed under the Apache License Version 2.0, http://www.apache.org/licenses/LICENSE-2.0";
		final String warning_msg = "WARNING: " + APP_NAME + " is able to owerwrite the existing "
				+ APP_TNSNAMES_FILENAME + " file!\n\n";
		final String logging_msg = APP_NAME
				+ " provides logging functionality using Simple Logging Facade for Java (SLF4J) with a logback backend. "
				+ "Logback looks for a configuration file named " + APP_TNSSYNC_LOGBACK_FILENAME
				+ " in TNS_ADMIN directory.\n\n";
		final int width = 120;
		final PrintWriter writer = new PrintWriter(System.out);

		HelpFormatter formatter = new HelpFormatter();
		formatter.printUsage(writer, width, "java -jar " + APP_NAME + ".jar [-ta <DIR>]");
		formatter.printUsage(writer, width, "java -jar " + APP_NAME + ".jar -h");
		formatter.printUsage(writer, width, "java -jar " + APP_NAME + ".jar -v");
		formatter.printWrapped(writer, width, header);
		formatter.printWrapped(writer, width, warning_msg);
		formatter.printWrapped(writer, width, logging_msg);
		formatter.printOptions(writer, width, mOptions, 1, 3);
		formatter.printWrapped(writer, width, footer);
		writer.close();
	}

	private void printVersion() {
		System.out.println("Version: " + APP_NAME + "-" + APP_VERSION);
	}

	public Path getTnsAdminPath() {
		return Paths.get(mTnsAdminPathString);
	}

	public String getDefaultAdminContext() {
		return mDefaultAdminContext;
	}

	public String getDirectoryServers() {
		return mDirectoryServers;
	}
}
