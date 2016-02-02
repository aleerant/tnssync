package com.aleerant.tnssync;

public class TnsEntry implements Comparable<TnsEntry> {

	private String mNetServiceName;
	private String mNetDescriptionString;

	public TnsEntry(String netServiceName, String netDescriptionString) {
		mNetServiceName = netServiceName.trim().toUpperCase();
		mNetDescriptionString = netDescriptionString.trim();
	}

	public String getNetServiceName() {
		return mNetServiceName;
	}

	public String getNetDescriptionString() {
		return mNetDescriptionString;
	}

	public String getTnsNamesEntryFormat() {
		return this.mNetServiceName + " = " + mNetDescriptionString;
	}

	@Override
	public String toString() {
		return "TnsEntry [mNetServiceName=" + mNetServiceName + ", mNetDescriptionString=" + mNetDescriptionString
				+ "]";
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof TnsEntry)) {
			return false;
		}

		TnsEntry that = (TnsEntry) other;

		return this.mNetServiceName.equals(that.mNetServiceName)
				&& this.mNetDescriptionString.equals(that.mNetDescriptionString);
	}

	@Override
	public int hashCode() {
		int hashCode = 1;

		hashCode = hashCode * 37 + this.mNetServiceName.hashCode();
		hashCode = hashCode * 37 + this.mNetDescriptionString.hashCode();

		return hashCode;
	}

	@Override
	public int compareTo(TnsEntry other) {
		return this.mNetServiceName.compareTo(other.mNetServiceName);
	}

}
