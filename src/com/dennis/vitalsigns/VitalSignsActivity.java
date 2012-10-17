/*adb install <full path>\sensorsimulator-1.1.1\bin\SensorSimulatorSettings-1.1.1.apk

cd <fullpath>\android-sdk_r12-windows\android-sdk-windows\platform-tools
 * 
 * 
 * 
 * */

package com.dennis.vitalsigns;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.telephony.TelephonyManager;

/* author: dennisfreethinker[(at]]yahoo([dot)]com:
 */
public class VitalSignsActivity extends Activity {


	private static Button buttonStart;
	private static Button buttonStop;
	private Button buttonPreference;
	public static boolean startButtonStatus=true;//when one opens the app for the very first time

	public static Handler buttonStatusUpdateHandler;

	private Intent VitalSignsServiceIntent;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		try {

			super.onCreate(savedInstanceState);
			setContentView(R.layout.main);

			VitalSignsService.Log("in onCreate()");

			initializeScreenVariables();

			// get device id
			Context context = getApplicationContext();
			TelephonyManager tm = (TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE);
			String deviceID = tm.getDeviceId();


			updateButtonStatus();
			// just pass the reference to the service
			VitalSignsService.setMainActivity(this);
			VitalSignsServiceIntent = new Intent(this, VitalSignsService.class);
			initializeButtonListeners();

		} catch (Exception e) {
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		updateButtonStatus();
	}

	private void initializeScreenVariables() throws Exception {
		buttonStart = (Button) findViewById(R.id.buttonStart);
		buttonStop = (Button) findViewById(R.id.buttonStop);
		buttonPreference = (Button) findViewById(R.id.buttonPreference);

	}

	private void initializeButtonListeners() {

		buttonStart.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startApp();
			}
		});

		buttonStop.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				stopApp();
			}
		});


		
		buttonPreference.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                        Intent settingsActivity = new Intent(getBaseContext(),
                                        Preferences.class);
                        startActivity(settingsActivity);
                }
        });

		buttonStatusUpdateHandler = new Handler() {
			@Override
			public void dispatchMessage(Message msg) {
				super.dispatchMessage(msg);
				setStartButtonStatus(true);
				VitalSignsService.Log("buttons  reset");
			}
		};

	}

	private void startApp() {
		try {

			Calendar calExpiry = Calendar.getInstance();
			calExpiry.set(2015, Calendar.APRIL, 10);
			Calendar currentcal = Calendar.getInstance();

			if (currentcal.before(calExpiry)) {
				CommonMethods cm=new CommonMethods();
				// make sure the service is stopped before it's started again
				if (cm.isVitalSignsServiceRunning(this)) {
					VitalSignsService.Log("Stopping service");
					stopApp();
				}
				cm.scheduleRepeatingMonitoringSessions(this);
				cm.playBeep(100);
				setStartButtonStatus(false);
				// remove or hide the app
				// finish();
			} else {
				showMessage("This software is past it's expiration date. Please contact the developer");
			}

		} catch (Exception e) {
			VitalSignsService.releasePartialWakeLock();
			VitalSignsService.Log("Error: " + e);
			setStartButtonStatus(true);
		}
	}
	
	/*
	private void startApp() {
		try {
			MonitorAsyncTask mMonitorAsyncTask = new MonitorAsyncTask();
			mMonitorAsyncTask.execute();
		} catch (Exception e) {
			
		}
	}

	
	
	  private class MonitorAsyncTask extends AsyncTask<Void, Void, Void> {
		  
		  @Override
		  protected void onPreExecute(){
			  setStartButtonStatus(false);
		  }
		  
		  @Override
		  protected Void doInBackground(Void... args) {	

				CommonMethods cm=new CommonMethods();
				// make sure the service is stopped before it's started again
				if (cm.isVitalSignsServiceRunning(VitalSignsActivity.this)) {
					VitalSignsService.Log("Stopping service");
					stopApp();
				}
				cm.scheduleRepeatingMonitoringSessions(VitalSignsActivity.this);
				cm.playBeep(100);
				setStartButtonStatus(false);
			  return null;
		  }
		  
		  @Override
		protected void onProgressUpdate(Void... values) {
			  //do stuff here
			super.onProgressUpdate(values);// this line is always the last
		}

		@Override
		  protected void onPostExecute(Void result) {
			//do stuff here
		  }
		  
	  }
	
*/
	private void stopApp() {
		VitalSignsService.Log("stopService called");
		try { 
			setStartButtonStatus(true); // the loop is to be stopped using this variable
			CommonMethods cm=new CommonMethods();
			cm.playBeep(100);
			cm.cancelRepeatingMonitoringSessions(this);
			cm.removeNotification(this);	
			/* this does not stop the other methods in the service if they happened to be in a loop
			 * So the loop must be stopped by some other manner
			 */
			stopService(VitalSignsServiceIntent);			
			VitalSignsService.releasePartialWakeLock();
			if (!cm.isVitalSignsServiceRunning(this)) {
				setStartButtonStatus(true);
			}
			// remove or hide the app
			// finish();
		} catch (Exception e) {
			VitalSignsService.releasePartialWakeLock();
		}
	}
	


	public static void setStartButtonStatus(boolean status) {
		startButtonStatus=status;
		if (buttonStart != null && buttonStop != null) {
			buttonStart.setEnabled(status);
			buttonStop.setEnabled(!status);
		}
	}

	public void updateButtonStatus(){
		buttonStart.setEnabled(startButtonStatus);
		buttonStop.setEnabled(!startButtonStatus);
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