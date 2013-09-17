package com.dennis.vitalsigns;


import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;

/**
 * 
 * @author dennis
 * A class that does some tasks in the background so to take the load off Broadcast receivers
 */
public class GeneralService extends Service {


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		CommonMethods.Log("Power cord connected or disconnected.");

		String pluggedStatus =intent.getStringExtra("pluggedStatus");

		SharedPreferences settings=PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		long pauseTime=Integer.parseInt(settings.getString("key_chargingpausetime",getApplicationContext().getString(R.string.pref_charging_pausetime)));

		String toastMessage=null;
		long triggerAtTime=0;

		if(pluggedStatus!=null){
			if(pluggedStatus.equals("plugged")){
				toastMessage="VitalSigns will continue monitoring after "+ pauseTime + " minutes";
				triggerAtTime = System.currentTimeMillis() +pauseTime*60;
			}else if( pluggedStatus.equals("unplugged")){
				toastMessage="VitalSigns will now resume monitoring";
				triggerAtTime = 0;
			}

			if(!VitalSignsActivity.isStartButtonEnabled()){//process only if start button is in the pressed state
				Toast toast = Toast.makeText(getApplicationContext(),toastMessage, Toast.LENGTH_SHORT);
				toast.show();
				CommonMethods cm=new CommonMethods(getApplicationContext());
				cm.scheduleRepeatingMonitoringSessions(triggerAtTime);
			}
		}

		this.stopSelf();
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
