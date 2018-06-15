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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.LoggerFactory;

public class AppFileHandler implements APPCONSTANT {
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AppFileHandler.class);

	private static final String TNSNAMES_FILE_HEAD_MESSAGE = APP_AUTO_SECTION_MARK + " ##########################################\n"
			+ "#This is an automatically generated section, please do not modify it!\n#Edit "
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

	public List<TnsSyncEntry> getTnsSyncList() throws TnsSyncFileMissingException, AppException {
		LOGGER.debug("start getTnsSyncList from TnsSyncFile [{}]", mTnsSyncFilePath);
		List<TnsSyncEntry> resultEntries = new ArrayList<TnsSyncEntry>();
		Pattern patternSimple = Pattern.compile("^\\s*(\\w+)\\s*$");
		Matcher matcherSimple = patternSimple.matcher("");
		Pattern patternFullFormat = Pattern.compile("^\\s*(\\w+?)\\s*=\\s*(\\w+?)\\s*$");
		Matcher matcherFullFormat = patternFullFormat.matcher("");

		try (BufferedReader br = new BufferedReader(new FileReader(mTnsSyncFilePath.toFile()))) {
			for (String line; (line = br.readLine()) != null;) {
				LOGGER.trace("read line [{}]", line);
				if (line.trim().startsWith("#") || line.trim().isEmpty()) {
					continue;
				}

				matcherSimple.reset(line);
				if (matcherSimple.find()) {
					TnsSyncEntry entry = new TnsSyncEntry(matcherSimple.group(1), matcherSimple.group(1));
					resultEntries.add(entry);
					LOGGER.debug("found {}", entry.toString());
					continue;
				}
				
				matcherFullFormat.reset(line);
				if (matcherFullFormat.find()) {
					TnsSyncEntry entry = new TnsSyncEntry(matcherFullFormat.group(1), matcherFullFormat.group(2));
					resultEntries.add(entry);
					LOGGER.debug("found {}", entry.toString());
					continue;
				}

				LOGGER.debug("{} file is corrupt, line: \"{}\"", APP_TNSSYNC_FILENAME, line);
				throw new AppException(APP_TNSSYNC_FILENAME + " file is corrupt, line: \""+ line + "\"");
			}
		} catch (FileNotFoundException e) {
			throw new TnsSyncFileMissingException();
		} catch (IOException e) {
			throw new AppException("can not read " + APP_TNSSYNC_FILENAME + " file (" + mTnsSyncFilePath.toString()
					+ "), error message: " + e.getMessage() + ", caused by:" + e.getCause());
		}

		LOGGER.debug("end getTnsSyncList, result {}", resultEntries.toString());
		return resultEntries;
	}

	public List<TnsEntry> getCurrentTnsEntryList() throws AppException {
		LOGGER.debug("start getCurrentTnsEntryList: reading of net service data from current tnsnames.ora [{}]",
				mTnsNamesFilePath.toFile());
		this.mCurrentTnsNamesCorrupt = false;
		List<TnsEntry> resultTnsEntries = new ArrayList<TnsEntry>();
		Pattern pattern = Pattern.compile("^\\s*(\\w+?)\\s*=\\s*(.*)");
		Matcher matcher = pattern.matcher("");
		boolean autoSectionFound = false;

		try (BufferedReader br = new BufferedReader(new FileReader(mTnsNamesFilePath.toFile()))) {
			for (String line; (line = br.readLine()) != null;) {
				LOGGER.trace("read line [{}]", line);
				
				if (!autoSectionFound) {
					if (line.trim().startsWith(APP_AUTO_SECTION_MARK)) autoSectionFound = true;
					continue;
				}
				
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
	
	public Path getTnsSyncFilePath() {
		return mTnsSyncFilePath;
	}

	public boolean isCurrentTnsNamesCorrupt() {
		return mCurrentTnsNamesCorrupt;
	}

	public void writeNetServiceDataToBuildFile(List<TnsEntry> tnsNames) throws AppException {
		LOGGER.debug("start writeNetServiceDataToBuildFile [{}]", mTnsTmpbuildFilePath.toString());
		try {
			java.util.Date date = new java.util.Date();
			BufferedReader br = new BufferedReader(new FileReader(mTnsNamesFilePath.toFile()));
			PrintWriter w = new PrintWriter(mTnsTmpbuildFilePath.toFile());
			
			for (String line; (line = br.readLine()) != null;) {
				LOGGER.trace("read line [{}]", line);
				
				if (!line.trim().startsWith(APP_AUTO_SECTION_MARK)) {
					w.println(line);
					continue;
				} else {
					break;
				}
			}
			br.close();
			
			w.println(TNSNAMES_FILE_HEAD_MESSAGE);
			w.println("#Modified: " + new Timestamp(date.getTime()));
			w.println();
			for (TnsEntry tnsName : tnsNames) {
				w.println(tnsName.getTnsNamesEntryFormat());
			}
			w.close();
		} catch (IOException e) {
			throw new AppException("can not create build file (" + mTnsTmpbuildFilePath.toString() 
					+ ") or read tnsnames.ora file ("
					+ mTnsNamesFilePath.toString()
					+ "), error message: "
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
