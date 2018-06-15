package com.aleerant.tnssync;

public class TnsSyncEntry implements Comparable<TnsSyncEntry> {
	
	private String mEntryName;
	private String mNetServiceName;

	public TnsSyncEntry(String entryName, String netServiceName) {
		mEntryName = entryName.trim().toUpperCase();
		mNetServiceName = netServiceName.trim().toUpperCase();
	}

	public String getEntryName() {
		return mEntryName;
	}

	public String getNetServiceName() {
		return mNetServiceName;
	}

	public String getTnsSyncEntryFormat() {
		return this.mEntryName + " = " + this.mNetServiceName;
	}

	@Override
	public String toString() {
		return "TnsSyncEntry [mEntryName=" + mEntryName + ", mNetServiceName=" + mNetServiceName
				+ "]";
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof TnsSyncEntry)) {
			return false;
		}

		TnsSyncEntry that = (TnsSyncEntry) other;

		return this.mEntryName.equals(that.mEntryName)
				&& this.mNetServiceName.equals(that.mNetServiceName);
	}

	@Override
	public int hashCode() {
		int hashCode = 1;

		hashCode = hashCode * 37 + this.mEntryName.hashCode();
		hashCode = hashCode * 37 + this.mNetServiceName.hashCode();

		return hashCode;
	}

	@Override
	public int compareTo(TnsSyncEntry other) {
		return this.mEntryName.compareTo(other.mEntryName);
	}
}
