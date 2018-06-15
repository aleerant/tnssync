/*
 * Copyright 2016 Attila Lerant, aleerantdev@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aleerant.tnssync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class TnsSyncFromLdap implements APPCONSTANT {
	private static LdapHandler mLdapHandler;
	private static AppFileHandler mFileHandler;
	private static PropertiesHandler mPropertiesHandler;

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TnsSyncFromLdap.class);

	public static void main(String[] args) {
		Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		root.setLevel(Level.OFF);

		try {
			mPropertiesHandler = new PropertiesHandler(args);
			MDC.put("tnsadmin", mPropertiesHandler.getTnsAdminPath().toString());
			LOGGER.info(String.format("start (%s-%s)", APP_NAME, APP_VERSION));
			mFileHandler = new AppFileHandler(mPropertiesHandler.getTnsAdminPath());
			mLdapHandler = new LdapHandler(mPropertiesHandler.getDirectoryServers(),
					mPropertiesHandler.getDefaultAdminContext());

			try {
				List<TnsSyncEntry> tnsSyncList = mFileHandler.getTnsSyncList();
				Map<String, TnsEntry> tnsDataFromLdap = mLdapHandler.queryTnsEntryMap(getUniqueNetServiceNameList(tnsSyncList));
				List<TnsEntry> tnsEntryListFromLdap = createTnsEntryListForTnsSyncEntries(tnsSyncList, tnsDataFromLdap);
				List<TnsEntry> tnsEntryListCurrent = mFileHandler.getCurrentTnsEntryList();

				if (mFileHandler.isCurrentTnsNamesCorrupt()
						|| !equalLists(tnsEntryListFromLdap, tnsEntryListCurrent)) {
					mFileHandler.writeNetServiceDataToBuildFile(tnsEntryListFromLdap);
					mFileHandler.moveBuidFileToFinal();
					LOGGER.info("new tnsnames.ora file created (" + (tnsEntryListFromLdap.size() < 2
							? tnsEntryListFromLdap.size() + " entry" : tnsEntryListFromLdap.size() + " entries")
							+ ")");
				} else {
					LOGGER.info("nothing to do");
				}

			} catch (TnsSyncFileMissingException e) {
				LOGGER.warn("{} file is missing [{}]", APP_TNSSYNC_FILENAME, mFileHandler.getTnsSyncFilePath().toString());
			}
			LOGGER.info("finished");

		} catch (AppException e) {
			// e.printStackTrace();
			LOGGER.error(e.getMessage());
			System.exit(1);
		} catch (Exception e) {
			// e.printStackTrace();
			LOGGER.error(Utils.getStackTrace(e));
			System.exit(1);
		}

	}

	public static boolean equalLists(List<TnsEntry> one, List<TnsEntry> two) {
		if (one == null && two == null) {
			return true;
		}

		Collections.sort(one);
		Collections.sort(two);
		return one.equals(two);
	}
	
	public static List<String> getUniqueNetServiceNameList(List<TnsSyncEntry> tnsSyncEntries) {
		LOGGER.debug("start getUniqueNetServiceNameList");
		Set<String> hs = new HashSet<>();

		for (TnsSyncEntry tnsSyncEntry : tnsSyncEntries) {
			hs.add(tnsSyncEntry.getNetServiceName());
		}
		List<String> resultList = new ArrayList<String>();
		resultList.addAll(hs);
		LOGGER.debug("end getUniqueNetServiceNameList, result {}", resultList.toString());
		return resultList;		
	}
	
	public static List<TnsEntry> createTnsEntryListForTnsSyncEntries(
			List<TnsSyncEntry> tnsSyncEntries, Map<String, TnsEntry> tnsEntries) {
		LOGGER.debug("start createTnsEntryListForTnsSyncEntries");
		List<TnsEntry> resultList = new ArrayList<TnsEntry>();
		
		LOGGER.debug("creating ordered Map for tnsSyncEntries");
		Map<String, TnsSyncEntry> om = new TreeMap<String, TnsSyncEntry>();
		for (TnsSyncEntry tnsSyncEntry : tnsSyncEntries) { 
			om.put(tnsSyncEntry.getEntryName(), tnsSyncEntry);
		}
		LOGGER.debug("Map created, result {}", om.toString());
		
		for (Map.Entry<String, TnsSyncEntry> tnsSyncEntry : om.entrySet()) {
			LOGGER.debug(" processing tnsSyncEntry: {}", tnsSyncEntry.toString());
			TnsEntry tnsEntry = tnsEntries.get(tnsSyncEntry.getValue().getNetServiceName());
			if (tnsEntry != null) {
				LOGGER.debug("  netServiveName ({}) is found, tnsEntry: {}", tnsSyncEntry.getValue().getNetServiceName(), tnsEntry.toString());
				resultList.add(new TnsEntry(tnsSyncEntry.getValue().getEntryName(),tnsEntry.getNetDescriptionString()));
			}
		}

		LOGGER.debug("end createTnsEntryListForTnsSyncEntries, result {}", resultList.toString());
		return resultList;		
	}
}
