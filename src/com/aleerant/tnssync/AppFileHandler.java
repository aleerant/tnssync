package com.aleerant.tnssync;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.LoggerFactory;

public class AppFileHandler implements APPCONSTANT {
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AppFileHandler.class);

	private static final String TNSNAMES_FILE_HEAD_MESSAGE = "#This is an automatically generated file, please do not modify it!\n#Edit "
			+ APP_TNSSYNC_FILENAME + " file instead of this!";

	private Path mTnsAdminPath, mTnsSyncFilePath, mTnsTmpbuildFilePath, mTnsNamesFilePath;
	private boolean mCurrentTnsNamesCorrupt;

	public AppFileHandler(Path tnsAdminPath) {
		LOGGER.debug("start construction of AppFileHandler class [parameter tnsAdminPath={}]", tnsAdminPath.toString());
		mTnsAdminPath = tnsAdminPath;
		mTnsSyncFilePath = Paths.get(mTnsAdminPath.toString(), APP_TNSSYNC_FILENAME);
		mTnsTmpbuildFilePath = Paths.get(mTnsAdminPath.toString(), "tnsnames.tmpbuild.ora");
		mTnsNamesFilePath = Paths.get(mTnsAdminPath.toString(), "tnsnames.ora");

		LOGGER.debug("AppFileHandler properties: [TnsAdminPath        = {}]", mTnsAdminPath.toString());
		LOGGER.debug("AppFileHandler properties: [TnsSyncFilePath     = {}]", mTnsSyncFilePath.toString());
		LOGGER.debug("AppFileHandler properties: [TnsTmpbuildFilePath = {}]", mTnsTmpbuildFilePath.toString());
		LOGGER.debug("AppFileHandler properties: [TnsNamesFilePath    = {}]", mTnsNamesFilePath.toString());
	}

	public List<String> getTnsSyncList() throws TnsSyncFileMissingException {
		LOGGER.debug("start getTnsSyncList from TnsSyncFile [{}]", mTnsSyncFilePath);
		Scanner s;
		Set<String> hs = new HashSet<>();
		try {
			s = new Scanner(mTnsSyncFilePath.toFile());

			String word;
			while (s.hasNext()) {
				word = s.next().toUpperCase().replaceAll("\\s", "");
				LOGGER.trace("process word, [{}]", word);
				if (word.startsWith("#")) {
					s.nextLine();
					LOGGER.trace("skip remaining in line");
				} else {
					hs.add(word);
					LOGGER.trace("add [{}]", word);
				}
			}

			s.close();
		} catch (FileNotFoundException e) {
			LOGGER.warn("{} file is missing [{}]", APP_TNSSYNC_FILENAME, mTnsSyncFilePath.toString());
			throw new TnsSyncFileMissingException();
		}
		List<String> tnsSyncList = new ArrayList<String>();
		tnsSyncList.addAll(hs);
		LOGGER.debug("end getTnsSyncList, result {}", tnsSyncList.toString());
		return tnsSyncList;
	}

	public List<TnsEntry> getCurrentTnsEntryList() throws AppException {
		LOGGER.debug("start getCurrentTnsEntryList: reading of net service data from current tnsnames.ora [{}]",
				mTnsNamesFilePath.toFile());
		this.mCurrentTnsNamesCorrupt = false;
		List<TnsEntry> resultTnsEntries = new ArrayList<TnsEntry>();
		Pattern pattern = Pattern.compile("^\\s*(\\w+?)\\s*=\\s*(.*)");
		Matcher matcher = pattern.matcher("");

		try (BufferedReader br = new BufferedReader(new FileReader(mTnsNamesFilePath.toFile()))) {
			for (String line; (line = br.readLine()) != null;) {
				LOGGER.trace("read line [{}]", line);
				if (line.trim().startsWith("#") || line.trim().isEmpty()) {
					continue;
				}

				matcher.reset(line);
				if (matcher.find()) {
					TnsEntry entry = new TnsEntry(matcher.group(1), matcher.group(2));
					resultTnsEntries.add(entry);
					LOGGER.debug("found {}", entry.toString());
					continue;
				}

				this.mCurrentTnsNamesCorrupt = true;
				LOGGER.debug("tnsnames.ora file is corrupt.");
				break;
			}
		} catch (FileNotFoundException e) {
			LOGGER.debug("current tnsnames.ora file is missing [{}]", mTnsNamesFilePath.toString());
		} catch (IOException e) {
			throw new AppException("can not read tnsnames.ora file (" + mTnsNamesFilePath.toString()
					+ "), error message: " + e.getMessage() + ", caused by:" + e.getCause());
		}

		LOGGER.debug("end getCurrentTnsEntryList");
		return resultTnsEntries;
	}

	public boolean isCurrentTnsNamesCorrupt() {
		return mCurrentTnsNamesCorrupt;
	}

	public void writeNetServiceDataToBuildFile(List<TnsEntry> tnsNames) throws AppException {
		LOGGER.debug("start writeNetServiceDataToBuildFile [{}]", mTnsTmpbuildFilePath.toString());
		try {
			java.util.Date date = new java.util.Date();
			PrintWriter writer;
			writer = new PrintWriter(mTnsTmpbuildFilePath.toFile());
			writer.println(TNSNAMES_FILE_HEAD_MESSAGE);
			writer.println("#Modified: " + new Timestamp(date.getTime()));
			writer.println();
			for (TnsEntry tnsName : tnsNames) {
				writer.println(tnsName.getTnsNamesEntryFormat());
			}
			writer.close();
		} catch (FileNotFoundException e) {
			throw new AppException("can not write build file (" + mTnsTmpbuildFilePath.toString() + "), error message: "
					+ e.getMessage() + ", caused by:" + e.getCause());
		}
		LOGGER.debug("end writeNetServiceDataToBuildFile");
	}

	public void moveBuidFileToFinal() throws AppException {
		LOGGER.debug("start moveBuidFileToFinal [buildfile={}, targetfile={}]", mTnsTmpbuildFilePath.toString(),
				mTnsNamesFilePath.toString());
		try {
			Files.move(mTnsTmpbuildFilePath, mTnsNamesFilePath, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new AppException("can not move build file to final tnsnames.ora (buildfile: "
					+ mTnsTmpbuildFilePath.toString() + ", target: " + mTnsNamesFilePath.toString()
					+ "), error message: " + e.getMessage() + ", caused by:" + e.getCause());
		}

	}
}
