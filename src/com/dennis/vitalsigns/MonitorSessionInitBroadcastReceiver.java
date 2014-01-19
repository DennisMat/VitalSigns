package com.dennis.vitalsigns;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
/*This class is what is  called by the Alarm manager periodically.
 */
public class MonitorSessionInitBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {		
		try {
			CommonMethods mCommonMethods=new CommonMethods(context);
			CommonMethods.aquirePartialWakeLock(context);
			CommonMethods.Log("in MonitorSessionInitBroadcastReceiver");
			if(!(mCommonMethods.isVitalSignsServiceRunning())){
			Intent VitalSignsServiceIntent = new Intent(context, VitalSignsService.class);
			context.startService(VitalSignsServiceIntent);			
			}
		} catch (Exception e) {
			CommonMethods.releasePartialWakeLock();
		}
	}

	
	
	
	

}
