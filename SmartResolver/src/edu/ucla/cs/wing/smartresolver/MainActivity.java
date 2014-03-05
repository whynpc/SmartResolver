package edu.ucla.cs.wing.smartresolver;

import java.io.IOException;

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
	
	private ToggleButton toggleButtonResolver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		toggleButtonResolver = (ToggleButton) findViewById(R.id.toggleButtonResolver);
		toggleButtonResolver.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					BackgroundService.getResolver().start();					
				} else {
					BackgroundService.getResolver().stop();					
				}
			}
		});
		
		
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
		try {
			Runtime.getRuntime().exec("ping -c 3 short.dnstest.whynpc.info");
		} catch (IOException e) {

		}
	}
	
	private void sendMsgToService() {
		
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
