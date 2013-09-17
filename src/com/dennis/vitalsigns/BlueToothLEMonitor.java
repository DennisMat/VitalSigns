package com.dennis.vitalsigns;

import android.content.Context;

public class BlueToothLEMonitor {

	Preferences pref;
	private Context context;
	
	public BlueToothLEMonitor(Context context){
		this.context=context;
	}
	
	public boolean getPersonEmergencyStatus() {
		
		return true;
	}
	
	
}
