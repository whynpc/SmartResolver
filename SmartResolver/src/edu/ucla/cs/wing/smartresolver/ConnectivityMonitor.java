package edu.ucla.cs.wing.smartresolver;


import edu.ucla.cs.wing.smartresolver.EventLog.LogType;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class ConnectivityMonitor extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		
		Bundle bundle = intent.getExtras();
		
		//EventLog.write(LogType.DEBUG, "connectivity change");
		
		for (String key : bundle.keySet()) {
			//EventLog.write(LogType.DEBUG, key + ": " + bundle.get(key).toString());
		}
		DnsResolver resolver =  BackgroundService.getResolver();
		if (resolver != null) {
			resolver.onNetworkChange();
		}
	}

}
