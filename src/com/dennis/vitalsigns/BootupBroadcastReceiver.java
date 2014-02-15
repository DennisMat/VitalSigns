package com.dennis.vitalsigns;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**The alarm manager does not survive a re-boot hence this class.
 * 
 * @author dennis
 *
 */
public class BootupBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			CommonMethods mCommonMethods=new CommonMethods(context);
			CommonMethods.aquirePartialWakeLock(context);
			CommonMethods.Log("in BootupBroadcastReceiver");
			if(!(mCommonMethods.isVitalSignsServiceRunning())){
				if(!mCommonMethods.getFlagShutDown()){
					mCommonMethods.scheduleRepeatingMonitoringSessions();
				}
			}
		} catch (Exception e) {
			CommonMethods.releasePartialWakeLock();
		}
	}

	
	
	
	

}
