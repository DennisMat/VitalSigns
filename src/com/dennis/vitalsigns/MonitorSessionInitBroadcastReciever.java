package com.dennis.vitalsigns;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
/*This class is what is  called by the Alarm manager periodically.
 */
public class MonitorSessionInitBroadcastReciever extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {		
		try {
			VitalSignsService.aquirePartialWakeLock(context);
			VitalSignsService.Log("in MonitorSessionInitBroadcastReciever");
			if(!((new CommonMethods()).isVitalSignsServiceRunning(context))){
			Intent VitalSignsServiceIntent = new Intent(context, VitalSignsService.class);
			context.startService(VitalSignsServiceIntent);			
			}
		} catch (Exception e) {
			VitalSignsService.releasePartialWakeLock();
		}
	}

	
	
	
	

}
