package com.dennis.vitalsigns;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.widget.Toast;


public class PowerConnectionBroadcastReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		VitalSignsService.Log("Power cord eonnected or disconnected.");
		SharedPreferences settings=PreferenceManager.getDefaultSharedPreferences(context);
		long pauseTime=Integer.parseInt(settings.getString("key_chargingpausetime",context.getString(R.string.pref_charging_pausetime)));
		String action = intent.getAction();    	
		int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

		String toastMessage="";
		long triggerAtTime=0;
		
		if( (status == BatteryManager.BATTERY_STATUS_CHARGING) || action.equals(Intent.ACTION_POWER_CONNECTED)){ //this could be either BATTERY_PLUGGED_USB or BATTERY_PLUGGED_AC
				toastMessage="VitalSigns will continue monitoring after "+ pauseTime + " minutes";
				triggerAtTime = System.currentTimeMillis() +pauseTime*60;
		}else if( action.equals(Intent.ACTION_POWER_DISCONNECTED)){
			toastMessage="VitalSigns will continue monitoring";
			triggerAtTime = 0;
		}
		
		if(!VitalSignsActivity.startButtonStatus){//process only if start button is in the pressed state
			Toast toast = Toast.makeText(context,toastMessage, Toast.LENGTH_SHORT);
			toast.show();
			CommonMethods cm=new CommonMethods();
			cm.scheduleRepeatingMonitoringSessions(context,triggerAtTime);
		}


	}
}