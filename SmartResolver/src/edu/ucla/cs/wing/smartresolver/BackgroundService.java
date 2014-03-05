package edu.ucla.cs.wing.smartresolver;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class BackgroundService extends Service {
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		init();
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	private void init() {
		
	}
	
	
	private void sendMsgToActivity() {
		
	}

}
