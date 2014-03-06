package edu.ucla.cs.wing.smartresolver;

import org.xbill.DNS.Resolver;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class BackgroundService extends Service {
	
	private static DnsResolver _resolver;
	
	private SharedPreferences prefs;
	private MobileInfo mobileInfo;
	
	public static DnsResolver getResolver() {
		return _resolver;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		MobileInfo.init(this);
		mobileInfo = MobileInfo.getInstance();
		
		_resolver = new DnsResolver(this, prefs);
		_resolver.init();
		
		DnsProxy.deployDnsProxy(this);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (_resolver != null) {
			_resolver.cleanUp();
		}
	}
	
	private void sendMsgToActivity() {
		
	}

}
