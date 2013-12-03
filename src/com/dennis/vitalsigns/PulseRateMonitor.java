package com.dennis.vitalsigns;


import java.io.*;
import java.net.*;

import android.bluetooth.*;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class PulseRateMonitor {


    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
	Preferences pref;
	private Context context;
	CommonMethods mCommonMethods=null;
	public PulseRateMonitor(Context context){
		this.context=context;
		mCommonMethods= new CommonMethods(context);
	}
	
	public boolean getPersonPulseEmergencyStatus() {

		SharedPreferences settings=PreferenceManager.getDefaultSharedPreferences(context);
		int pulseRateLow=Integer.parseInt(settings.getString("key_pulse_rate_low",context.getString(R.string.pref_pulse_rate_low)));
		int pulseRateHigh=Integer.parseInt(settings.getString("key_pulse_rate_high",context.getString(R.string.pref_pulse_rate_high)));
		int pulseRate= getPulseRate();
		CommonMethods.Log("pulseRateLow=" + pulseRateLow + " pulseRate=" + pulseRate + " pulseRateHigh=" + pulseRateHigh);
		if((pulseRate<=pulseRateLow) || (pulseRate>=pulseRateHigh)){
			return true;
		}else{
			return false;
		}

	} 
	
	private void startScanning(){
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
        	//mCommonMethods.showToast( R.string.ble_not_supported, Toast.LENGTH_SHORT);
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
        	//.showToast(R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT);
            return;
        }
	}
	
	private int getPulseRate(){
		int pulseRate=0;
		CommonMethods.Log("Obtaining pulse rate ...");
		try {
			Thread.sleep(10000);
			URL url = new URL("http://photonshift.com/pulse.txt");

			BufferedReader in = new BufferedReader(
			new InputStreamReader(url.openStream()));

			String inputLine;
			while ((inputLine = in.readLine()) != null){
				CommonMethods.Log("pulse rate read form file is " + inputLine);
				pulseRate=Integer.parseInt(inputLine);
			}
			in.close();
			
		} catch (Exception e) {

		}
		
		return pulseRate;
	}
	
	
}
