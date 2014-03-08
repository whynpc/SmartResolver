package edu.ucla.cs.wing.smartresolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {
	
	private static Pattern patternCPU = Pattern
			.compile("\\s*(\\d+)\\s+(\\d+)%\\s+.*\\s+([^\\s]+)");
	
	public static String ip2block(String ip) {		
		ip.lastIndexOf('.');
		
		return null;
	}
	
	private int getCpuUtilization() {
		int utilization = 0;

		BufferedReader in = null;

		try {
			Process process = null;
			process = Runtime.getRuntime().exec("top -n 1 -d 0.");

			in = new BufferedReader(new InputStreamReader(
					process.getInputStream()));

			String line = "";
			while ((line = in.readLine()) != null) {
				Matcher matcher = patternCPU.matcher(line);
				if (matcher.find()) {
					int pid = Integer.parseInt(matcher.group(1).toString());
					int cpuUsage = Integer.parseInt(matcher.group(2)
							.toString());
					String app = matcher.group(3).toString();
					if (!app.equals("top")) {
						utilization += cpuUsage;
					}

				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return utilization;
	}

}
