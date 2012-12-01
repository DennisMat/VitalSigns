package com.dennis.vitalsigns;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

/**
 * 
 * @author dennis
 *Buggy not tested.
 */
public class PowerConnectionBroadcastReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();    	
		int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		String pluggedStatus= "";
		
		if( (status == BatteryManager.BATTERY_STATUS_CHARGING) || action.equals(Intent.ACTION_POWER_CONNECTED)){ //this could be either BATTERY_PLUGGED_USB or BATTERY_PLUGGED_AC
			pluggedStatus= "plugged";
		}else if( action.equals(Intent.ACTION_POWER_DISCONNECTED)){
			pluggedStatus= "unplugged";
		}
		
		// Dennis we dispatch off the work to a service so that this class does not have too much code.
		Intent GeneralServicesIntent = new Intent(context, GeneralService.class);		
		GeneralServicesIntent.putExtra("pluggedStatus", pluggedStatus);
		context.startService(GeneralServicesIntent);

	}
}