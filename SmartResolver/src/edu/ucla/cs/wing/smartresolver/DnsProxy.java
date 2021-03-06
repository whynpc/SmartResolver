package edu.ucla.cs.wing.smartresolver;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import edu.ucla.cs.wing.smartresolver.EventLog.LogType;
import android.content.Context;

public class DnsProxy {

	public static final String PROXY_BIN_PATH = "/data/local/dnsproxy";
	public static final String CMD_LAUNCH_PROXY = "su -c ./data/local/dnsproxy";
	public static final String CMD_STOP_PROXY = "su -c killall dnsproxy";

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
			os.writeBytes("chmod 777 /data/local/dnsproxy\n");

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
	
	public static boolean isDnsProxyRunning() {
		return false;
	}

	public static void launchDnsProxy() {
		try {
			if (!isDnsProxyRunning()) {
				Runtime.getRuntime().exec(CMD_LAUNCH_PROXY);				
			}
		} catch (IOException e2) {
			EventLog.write(LogType.ERROR, "Fail to launch proxy");
		}
	}

	public static void changeDnsServerSetting() {
		// change DNS server
		try {
			// only change primary server; keep secondary server as backup
			String cmd = "su -c setprop net.dns1 127.0.0.1";
			Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			EventLog.write(LogType.ERROR, "Fail to change dns server setting");
		}
	}
	
	public static void restoreDnsServerSetting() {
		String cmd = "su -c setprop net.dns1 "
				+ (MobileInfo.getInstance().isConnectingWifi() ? MobileInfo
						.getInstance().getWifiDnsServer(1) : MobileInfo
						.getInstance().getCellularDnsServer(1));
		try {
			Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			EventLog.write(LogType.ERROR, "Fail to restore dns server setting");
		}		
	}

	public static void stopDnsProxy() {
		try {

			Runtime.getRuntime().exec(CMD_STOP_PROXY);
		} catch (IOException e1) {
			EventLog.write(LogType.ERROR, "Fail to stop proxy");
		}

	}

}
