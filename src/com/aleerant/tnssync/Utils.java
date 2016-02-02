package com.aleerant.tnssync;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public final class Utils {
	public static String getStackTrace(Throwable aThrowable) {
	    Writer result = new StringWriter();
	    PrintWriter printWriter = new PrintWriter(result);
	    aThrowable.printStackTrace(printWriter);
	    return result.toString();
	  }
}
