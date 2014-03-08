package edu.ucla.cs.wing.smartresolver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.os.Bundle;
import android.print.PrintAttributes.Resolution;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
	
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		
		
		
		Intent intent = new Intent(this, BackgroundService.class);
		startService(intent);		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void onClickDebug(View view) {
		new Thread() {
			@Override
			public void run() {
				try {
					InetAddress.getAllByName("short.dnstest.whynpc.info");
				} catch (UnknownHostException e) {				
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	private void sendMsgToService() {
		
	}
	
	public void onClickClearCache(View view) {
		BackgroundService.getResolver().clearCache();
	}
	
	public void onClickStartLog(View view) {
		String[] parameters = {String.valueOf(System.currentTimeMillis())};		
		EventLog.newLogFile(EventLog.genLogFileName(parameters));		
	}
	
	public void onClickStopLog(View view) {
		EventLog.close();
	}
	
	public void onClickStart(View view) {
		BackgroundService.getResolver().start();
	}
	
	public void onClickStop(View view) {
		BackgroundService.getResolver().stop();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			startActivity(new Intent(this, SettingsActivity.class));
			break;
		default:
			break;		
		}
		return true;
	}

}
