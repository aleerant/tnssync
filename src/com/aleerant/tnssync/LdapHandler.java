package com.aleerant.tnssync;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import org.slf4j.LoggerFactory;

public class LdapHandler {
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LdapHandler.class);

	private String mAdminContext;
	private String mProviderURLs;
	private LdapContext mCtx;

	// constructor
	public LdapHandler(String providerURLs, String adminContext) throws AppException {
		LOGGER.debug("start construction of LdapHandler class [providerURLs={}, adminContext={}]", providerURLs,
				adminContext);
		this.mAdminContext = adminContext;
		this.mProviderURLs = providerURLs;
	}

	private LdapContext getCtx() throws AppException {
		if (this.mCtx == null) {
			LOGGER.debug("start creating LdapContext [providerURLs={}]", this.mProviderURLs);
			Hashtable<String, Object> env = new Hashtable<String, Object>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			env.put(Context.SECURITY_AUTHENTICATION, "none");
			Iterator<String> i = getProviderUrlList(this.mProviderURLs).iterator();
			while (i.hasNext()) {
				env.put(Context.PROVIDER_URL, "ldap://" + i.next());
				try {
					LOGGER.debug("connecting to LDAP server [{}]", env.toString());
					this.mCtx = new InitialLdapContext(env, null);
					LOGGER.debug("connected to LDAP server");
					break;
				} catch (NamingException e) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("connection failed to LDAP server [{}]", env.toString());
						LOGGER.debug(Utils.getStackTrace(e));
					} else {
						LOGGER.warn("connection failed to LDAP server [{}], Caused by: {}", e.getMessage(),
								e.getCause());
					}
				}
			}

			if (this.mCtx == null) {
				// connection failed
				throw new AppException("failed to connect any LDAP Server [providerURLs: " + this.mProviderURLs + "]");
			}
		}
		return this.mCtx;
	}

	private List<String> getProviderUrlList(String providerURLs) {
		return Arrays.asList(providerURLs.replaceAll("[()\\s]", "").split(","));
	}

	public List<TnsEntry> queryTnsEntryList(List<String> filterCnList) throws AppException {
		LOGGER.debug(
				"start queryTnsEntryList: querying of net service data (objectClass: orclNetService) from ldap server");
		NamingEnumeration<SearchResult> namingEnum;
		List<TnsEntry> resultTnsEntries = new ArrayList<TnsEntry>();

		if (filterCnList != null && filterCnList.size() > 0) {
			try {
				namingEnum = getCtx().search(this.mAdminContext, "(objectClass=orclNetService)",
						getSimpleSearchControls());
				while (namingEnum.hasMore()) {
					SearchResult result = (SearchResult) namingEnum.next();
					Attributes attrs = result.getAttributes();
					if (filterCnList.contains(attrs.get("cn").get().toString().toUpperCase())) {
						TnsEntry entry = new TnsEntry(attrs.get("cn").get().toString(),
								attrs.get("orclNetDescString").get().toString());
						resultTnsEntries.add(entry);
						LOGGER.debug("found {}", entry.toString());
					}
				}
				namingEnum.close();
			} catch (NamingException e) {
				throw new AppException("failed to query TnsEntryList from LDAP Server, error message: " + e.getMessage()
						+ ", Caused by:" + e.getCause());
			}
		} else {
			LOGGER.debug("list is empty, skip search");
		}

		if (resultTnsEntries.size() > 0) {
			Collections.sort(resultTnsEntries);
		}
		LOGGER.debug("end queryTnsEntryList");
		return resultTnsEntries;
	}

	private SearchControls getSimpleSearchControls() {
		SearchControls sc = new SearchControls();
		sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
		return sc;

	}
}
