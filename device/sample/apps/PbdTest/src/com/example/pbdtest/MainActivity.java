package com.example.pbdtest;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PbdLocation;
import android.util.Log;
import android.widget.Toast;

//Import the pbd Library
import com.example.android.pbd.PbdLocationManager;
import com.example.android.pbd.PbdDeviceManager;

public class MainActivity extends Activity {

	private PbdLocationManager pbdLocManager;
	private PbdDeviceManager pbdDevManager;
	private final String TAG = "PbdTest";
	private MyReceiver receiver;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.d(TAG, "into Oncreate method");

		pbdLocManager = new PbdLocationManager(getApplicationContext());
		pbdDevManager = new PbdDeviceManager(getApplicationContext());

		//ADD TESTING CODE HERE

	}


	@Override
	protected void onStop(){
		super.onStop();
		//unregister the receiver and call onStop in PbdLocManager
		unregisterReceiver(receiver);
		pbdLocManager.pbdOnStop();
	}


	public class MyReceiver extends BroadcastReceiver {
	
		@Override
		public void onReceive(Context context, Intent intent) {

				try{
				PbdLocation pbdLoc = pbdLocManager.getPbdLocation();
				double lat = pbdLoc.getLatitude();
				double lon = pbdLoc.getLongitude();
				String l = "Pbd Longitude: " + lon + " Latitude: " + lat;
				Toast.makeText(getApplicationContext(), l ,Toast.LENGTH_LONG).show();
				Log.d(TAG, "SUCCESS to call getLoc function");
			}
			catch (Exception e){
				Log.d(TAG, "FAILED to call getLoc function");
				e.printStackTrace();
			}
		}
	}
	
}
