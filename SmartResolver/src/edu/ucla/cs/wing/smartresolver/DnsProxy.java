package edu.ucla.cs.wing.smartresolver;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;

public class DnsProxy {

	public static final String PROXY_BIN_PATH = "/data/local/dnsproxy";

	public static void deployDnsProxy(Context context) {
		File proxyBin = new File(PROXY_BIN_PATH);
		if (proxyBin.exists()) {
			return;
		}

		InputStream is = context.getResources().openRawResource(R.raw.dnsproxy);
		File outf = new File(context.getCacheDir(), "dnsproxy");
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(outf);

			byte[] buffer = new byte[1024];
			int len;
			while ((len = is.read(buffer)) >= 0) {
				fos.write(buffer, 0, len);
			}
			fos.close();
		} catch (Exception e) {
		}

		try {
			// Preform su to get root privledges
			Process p;
			p = Runtime.getRuntime().exec("su");

			// Attempt to write a file to a root-only
			DataOutputStream os = new DataOutputStream(p.getOutputStream());
			os.writeBytes("cp " + outf.getAbsolutePath() + " /data/local\n");
			os.writeBytes("chmod 777 /data/local/ipspoof\n");

			// Close the terminal
			os.writeBytes("exit\n");
			os.flush();
			try {
				p.waitFor();

			} catch (InterruptedException e) {

			}
		} catch (IOException e) {

		}

	}

}
