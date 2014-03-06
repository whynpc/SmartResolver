package edu.ucla.cs.wing.smartresolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import android.os.Environment;
import android.util.Log;

public class EventLog {

	public static final String TAG = "resolver";
	public static final String SEPARATOR = ";";

	public enum LogType {
		DEBUG, ERROR, MONITOR
	};

	private static PrintWriter logFileWriter;

	private static boolean enabled = true;

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean enabled) {
		EventLog.enabled = enabled;
	}

	public static void close() {
		if (logFileWriter != null) {
			logFileWriter.flush();
			logFileWriter.close();
			logFileWriter = null;
		}
	}

	public static void newLogFile(String fileName) {
		if (logFileWriter != null) {
			logFileWriter.flush();
			logFileWriter.close();
		}
		try {
			File dir = new File(Environment.getExternalStorageDirectory()
					+ File.separator + "smartresolver");
			if (!dir.exists()) {
				dir.mkdir();
			}

			logFileWriter = new PrintWriter(new FileOutputStream(new File(
					dir.getAbsolutePath(), fileName)));
		} catch (FileNotFoundException e) {
			logFileWriter = null;
			write(LogType.DEBUG, "Fail to open log file: " + e.toString());
		}
	}

	public static String genLogFileName(String[] parameters) {
		StringBuilder sb = new StringBuilder();
		sb.append("res");
		for (String parameter : parameters) {
			sb.append("_");
			String p = parameter.replace('&', '-');
			sb.append(p);
		}
		sb.append(".txt");
		
		return sb.toString();
	}

	public static void write(LogType type, String data) {
		if (enabled) {
			StringBuilder sb = new StringBuilder();
			sb.append(System.currentTimeMillis());
			sb.append(SEPARATOR);
			sb.append(type);
			if (data != null) {
				sb.append(SEPARATOR);
				sb.append(data);
			}
			sb.append(SEPARATOR);

			if (type != LogType.MONITOR) {
				Log.d(TAG, sb.toString());
			}

			if (logFileWriter != null) {
				logFileWriter.println(sb.toString());
				logFileWriter.flush();
			}
		}
	}

}
