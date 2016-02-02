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

import java.util.Collections;
import java.util.List;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class TnsSyncFromLdap implements APPCONSTANT {
	private static LdapHandler mLdapHandler;
	private static AppFileHandler mFileHandler;
	private static PropertiesHandler mPropertiesHandler;
	private static List<String> mTnsSyncList;
	private static List<TnsEntry> mTnsEntryListFromLdap, mCurrentTnsEntryList;

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TnsSyncFromLdap.class);

	public static void main(String[] args) {
		Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		root.setLevel(Level.OFF);

		try {
			mPropertiesHandler = new PropertiesHandler(args);
			System.out.println(APP_NAME + "-" + APP_VERSION + " started...");
			LOGGER.info("start");
			mFileHandler = new AppFileHandler(mPropertiesHandler.getTnsAdminPath());
			mLdapHandler = new LdapHandler(mPropertiesHandler.getDirectoryServers(),
					mPropertiesHandler.getDefaultAdminContext());

			try {
				mTnsSyncList = mFileHandler.getTnsSyncList();
				mTnsEntryListFromLdap = mLdapHandler.queryTnsEntryList(mTnsSyncList);
				mCurrentTnsEntryList = mFileHandler.getCurrentTnsEntryList();

				if (mFileHandler.isCurrentTnsNamesCorrupt()
						|| !equalLists(mTnsEntryListFromLdap, mCurrentTnsEntryList)) {
					mFileHandler.writeNetServiceDataToBuildFile(mTnsEntryListFromLdap);
					mFileHandler.moveBuidFileToFinal();
					System.out.println("new tnsnames.ora file created (" + (mTnsEntryListFromLdap.size() < 2
							? mTnsEntryListFromLdap.size() + " entry" : mTnsEntryListFromLdap.size() + " entries")
							+ ")");
				} else {
					System.out.println("nothing to do");
				}

			} catch (TnsSyncFileMissingException e) {
				System.out.println(APP_TNSSYNC_FILENAME + " file is missing");
			}

			System.out.println("finished.");
			LOGGER.info("finished");

		} catch (AppException e) {
			System.err.println(e.getMessage());
			// e.printStackTrace();
			LOGGER.error(e.getMessage());
			System.exit(1);
		} catch (Exception e) {
			System.err.println(e.getMessage());
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
}
