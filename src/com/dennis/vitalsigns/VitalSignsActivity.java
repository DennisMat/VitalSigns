/*adb install <full path>\sensorsimulator-1.1.1\bin\SensorSimulatorSettings-1.1.1.apk

cd <fullpath>\android-sdk_r12-windows\android-sdk-windows\platform-tools
 * 
 * 
 * 
 * */

package com.dennis.vitalsigns;

import java.util.Calendar;

import com.dennis.vitalsigns.Monitors.Statuses;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;

/* author: dennis
 */
public class VitalSignsActivity extends Activity {


	private static Button buttonStart;
	private static Button buttonStop;
	public static boolean flagShutDown=true;
	private Button buttonPreference;
	private CommonMethods mCommonMethods = null;


	/**
	 * When app is running this is false.
	 */
	private static boolean isAppRunning=false;//when one opens the app for the very first time
	public static final String BUTTON_UPDATE = "buttonUpdate";

	private Intent VitalSignsServiceIntent;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		try {

			super.onCreate(savedInstanceState);
			setContentView(R.layout.main);

			CommonMethods.Log("in onCreate()");

			initializeVariables();

			// get device id
			Context context = getApplicationContext();
			TelephonyManager tm = (TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE);
			String deviceID = tm.getDeviceId();


			updateButtonStatus();
			VitalSignsServiceIntent = new Intent(this, VitalSignsService.class);
			initializeButtonListeners();

		} catch (Exception e) {
		}

	}


	private BroadcastReceiver receiverButtonStatusUpdateEvent = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			CommonMethods.Log("receiverButtonStatusUpdateEvent onReceive() called" );
			updateButtonStatus();
			
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
		//registerReceiver(receiverButtonStatusUpdateEvent, new IntentFilter(BUTTON_UPDATE));
		LocalBroadcastManager.getInstance(this).registerReceiver(receiverButtonStatusUpdateEvent, new IntentFilter(BUTTON_UPDATE));
		CommonMethods.Log("onResume called" );
		updateButtonStatus();
	}

	@Override
	protected void onPause() {
		CommonMethods.Log("onPause called" );
		//unregisterReceiver(receiverButtonStatusUpdateEvent);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverButtonStatusUpdateEvent);
		super.onPause();
	}

	private void initializeVariables() throws Exception {
		buttonStart = (Button) findViewById(R.id.buttonStart);
		buttonStop = (Button) findViewById(R.id.buttonStop);
		buttonPreference = (Button) findViewById(R.id.buttonPreference);
		mCommonMethods= new CommonMethods(this);
	}

	private void initializeButtonListeners() {

		buttonStart.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startApp();
			}
		});

		buttonStop.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				flagShutDown=true;
				stopApp();
			}
		});


		buttonPreference.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				Intent settingsActivity = new Intent(VitalSignsActivity.this,
						Preferences.class);
				startActivity(settingsActivity);
			}
		});

	}




	private void startApp() {
		try {
			CommonMethods.Log("VitalSignsActivity.startApp() called 1");
			Calendar calExpiry = Calendar.getInstance();
			calExpiry.set(2020, Calendar.APRIL, 10);
			Calendar currentcal = Calendar.getInstance();

			if (currentcal.before(calExpiry)) {
				// make sure the service is stopped before it's started again
				if (mCommonMethods.isVitalSignsServiceRunning()) {
					CommonMethods.Log("Stopping service");
					stopApp();
				}
				flagShutDown=false;
				mCommonMethods.scheduleRepeatingMonitoringSessions();// call call the service right away.
				mCommonMethods.playBeep(100);
				// remove or hide the app
				// finish();
			} else {
				showMessage("This software is past it's expiration date. Please contact the developer - and part with your wealth!");
			}
			CommonMethods.Log("VitalSignsActivity.startApp() called 2");
		} catch (Exception e) {			
			CommonMethods.releasePartialWakeLock();
			flagShutDown=true;
			CommonMethods.Log("Error: " + e);
		}
		updateButtonStatus();
	}


	private void stopApp() {
		CommonMethods.Log("stopApp called 1");
		try {
			flagShutDown=true;// 
			mCommonMethods.playBeep(100);
			mCommonMethods.cancelRepeatingMonitoringSessions();
			mCommonMethods.removeNotification();	

			/* this does not stop the other methods in the service if they happened to be in a loop
			 * So the loop must be stopped by some other manner. We use flagShutDown for this purpose. 
			 */
			stopService(VitalSignsServiceIntent);			
			CommonMethods.releasePartialWakeLock();
			CommonMethods.Log("stopApp called 2");
			updateButtonStatus();
			// remove or hide the app
			// finish();
		} catch (Exception e) {
			CommonMethods.releasePartialWakeLock();
		}
	}


	/**
	 * This method is called from checkAllTasksAndUpdateButtons only. It is not on the main UI thread. 
	 * @param pressed
	 */
	private void setStartButtonPressed(final boolean pressed) {
		CommonMethods.Log("in setStartButtonPressed()" );
		runOnUiThread(new Runnable() {// because this is always called from a thread.
		     public void run() {
		 		try {
					if (buttonStart != null && buttonStop != null) {				
						buttonStart.setEnabled(!pressed);
						buttonStop.setEnabled(pressed);
						if(pressed){
							CommonMethods.Log("setStartButtonPressed called and startButton is pressed down" );
						}else{
							CommonMethods.Log("setStartButtonPressed called and startButton is pressable" );
						}						
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

		    }
		});

	}

	/**
	 * When app is running:<br/>
	 * The isStartButtonEnabled returns false<br/>
	 * The Start button is in the disabled state
	 * 
	 */
	public static boolean isStartButtonEnabled(){
		return buttonStart.isEnabled();
	}

	private void updateButtonStatus(){
		/* the buttons may take time to update depending on what is running 
		in the background and we don't want to hold up the UI hence the thread.
		*/
		try {
			Runnable r1=new Runnable() {
				public void run() {
					CommonMethods.Log("in updateButtonStatus()" );
					checkAllTasksAndUpdateButtons();					
				}
			};
			new Thread(r1).start();			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * This method is not on the main UI thread
	 */
	public void checkAllTasksAndUpdateButtons(){
		
		for (int i = 0; i < 10; i++) {// periodically monitor the variables.			
			boolean allTasksNotRunning=false;
			CommonMethods.Log("Service running= " + mCommonMethods.isVitalSignsServiceRunning());
			CommonMethods.Log("isHeartRateMonitorRunning = " + Monitors.Statuses.isHeartRateMonitorRunning);
			CommonMethods.Log("isBodyTemperatureMonitorRunning = " + Monitors.Statuses.isBodyTemperatureMonitorRunning);
			if (!mCommonMethods.isVitalSignsServiceRunning()
					&& !Monitors.Statuses.isHeartRateMonitorRunning
					&& !Monitors.Statuses.isBodyTemperatureMonitorRunning
					){
				allTasksNotRunning=true;
			}
			if(allTasksNotRunning){
				CommonMethods.Log("Hallelujah! All task have ended");
			}

			if(flagShutDown && allTasksNotRunning){
				setStartButtonPressed(false);
				break;
			}else if(!flagShutDown){
				setStartButtonPressed(true);
				break;
			}else{//cases where flagShutDown is true and allTasksNotRunning is false
				// do not break, continue on the loop
				CommonMethods.Log("some tasks are  still running");
			}
			
			CommonMethods.Log("in loop " + i);
			try {
				Thread.sleep(10000);// 10 sec pause between checks.
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}
	
	private AlertDialog.Builder showMessage(String mess){
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this); 
		alertDialog.setMessage(mess);	      	
		alertDialog
		.setIcon(0)
		.setTitle("")
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				return; //don't do anything.
			}
		})
		.create();
		alertDialog.show();	
		return alertDialog;
	}

}
