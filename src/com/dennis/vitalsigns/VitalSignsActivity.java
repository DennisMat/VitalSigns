
package com.dennis.vitalsigns;

import java.util.Calendar;












import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.*;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;

/* author: dennis
 */
public class VitalSignsActivity extends Activity {


	private static Button buttonStart;
	private static Button buttonStop;
	/**
	 * A global variable used to co-ordinate various things throughout the app, for example:
	 * <br/> The enabled/disabled states of the start and stop button.
	 * <br/>The running of the services in the background
	 *  <br/> <br/> This variable is also stored in the local storage, in case the phone is rebooted.
	 */
	public static boolean flagShutDown=true;
	private Button buttonPreference;
	private LinearLayout LinearLayoutStartStop;
	private Button buttonScan;
	private TextView textViewMessages;

	private CommonMethods mCommonMethods = null;
	public static final String BUTTON_UPDATE = "buttonUpdate";
	private Intent VitalSignsServiceIntent;
	private BlueToothMethods mBlueToothMethods=null;
	Preferences pref=null;

	private Handler handlerScheduleMonitoringSessions;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		try {

			super.onCreate(savedInstanceState);
			setContentView(R.layout.main);

			CommonMethods.Log("in onCreate()");

			initializeVariables();
			initializeButtonListeners();
			checkForHeartRateDevice();
			checkForPhoneNumbers();

			// get device id
			Context context = getApplicationContext();
			TelephonyManager tm = (TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE);
			String deviceID = tm.getDeviceId();


			updateButtonStatus();
			VitalSignsServiceIntent = new Intent(this, VitalSignsService.class);


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
		LocalBroadcastManager.getInstance(this).registerReceiver(receiverButtonStatusUpdateEvent, new IntentFilter(BUTTON_UPDATE));
		CommonMethods.Log("onResume called" );
		updateButtonStatus();
		checkForHeartRateDevice();
		checkForPhoneNumbers();
	}

	@Override
	protected void onPause() {
		CommonMethods.Log("onPause called" );
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverButtonStatusUpdateEvent);
		super.onPause();
	}

	private void initializeVariables() throws Exception {
		buttonStart = (Button) findViewById(R.id.buttonStart);
		buttonStop = (Button) findViewById(R.id.buttonStop);
		buttonPreference = (Button) findViewById(R.id.buttonPreference);
		LinearLayoutStartStop=(LinearLayout) findViewById(R.id.LinearLayoutStartStop);		
		buttonScan= (Button) findViewById(R.id.buttonScan);
		textViewMessages=(TextView) findViewById(R.id.textViewMessages);
		mCommonMethods= new CommonMethods(this);		
		mBlueToothMethods= new BlueToothMethods(this);

		pref = new Preferences(this);
		pref.loadValuesFromStorage();

		handlerScheduleMonitoringSessions = new Handler(){
			@Override
			public void handleMessage(Message msg){
				if(msg.what == 0){
					flagShutDown=true;
					mCommonMethods.showMessage("This app does is currently not recieving the heart rate from your heart rate device.(" +
							mBlueToothMethods.getHeartRateDeviceName() + ")" +
							" Make sure:\n" +
							"-That your heart rate device is turned on and is transmitting the heart rate.\n" +
							"-That you have not changed your heart rate device. If you have changed it please set your new device through the " +
							"advanced settings section of this app");
				}else{
					mCommonMethods.scheduleRepeatingMonitoringSessions();// call call the service right away.
					//mCommonMethods.playBeep(100);
					updateButtonStatus();
					flagShutDown=false;
					mCommonMethods.showMessage("This app is now monitoring your heart rate. You may now move to another screen on your phone. "
							+ "This app will continue monitoring your heart rate even if you switch off your phone. "
							+ "If you wnat to stop this app come back to this screen and hit the Stop button");
				}
				mCommonMethods.setFlagShutDown(flagShutDown);
			}
		};

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


		buttonScan.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent deviceScanIntent = new Intent(VitalSignsActivity.this, DeviceScanActivity.class);
				startActivity(deviceScanIntent);
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


	private void checkForHeartRateDevice(){		
		if(mBlueToothMethods.isHeartRateDeviceSet()){
			buttonScan.setVisibility(View.GONE);
			LinearLayoutStartStop.setVisibility(View.VISIBLE);
		}else{
			buttonScan.setVisibility(View.VISIBLE);
			LinearLayoutStartStop.setVisibility(View.GONE);
		}
	}

	private void checkForPhoneNumbers(){
		String message="";
		boolean phoneNumbersExist=false;
		
		for (int i = 0; i < pref.phoneNumberArray.length; i++) {
			try {
				if (pref.phoneNumberArray[i]!=null && pref.phoneNumberArray[i].length()>1 
						&&(pref.dialArray[i] || pref.SMSArray[i])) {// either sms or dial should be set
					phoneNumbersExist=true;
					break;
				}
			} catch (Exception e) { 
				CommonMethods.Log("Exception " + e.getMessage());
			}
		}
		if(!phoneNumbersExist){
			message="You have not entered any phone numbers that may be alerted in an emergency.\n"
					+"or you may not have enabled SMS or dial for that number.\n"
					+ "Unless you are testing the app make sure that you set up phone numbers in the Settings->Contacts section";
		}
		textViewMessages.setText(message);
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
			 
								
				scheduleMonitoringifHeartRateReceiving();

			} else {
				mCommonMethods.showMessage("This software is past it's expiration date. Please contact the developer - and part with your wealth!");
			}
			CommonMethods.Log("VitalSignsActivity.startApp() called 2");
		} catch (Exception e) {			
			CommonMethods.releasePartialWakeLock();
			flagShutDown=true;
			mCommonMethods.setFlagShutDown(flagShutDown);
			CommonMethods.Log("Error: " + e);
		}

	}


	private void scheduleMonitoringifHeartRateReceiving(){

		final ProgressDialog progress = new ProgressDialog(this);		
		progress.setMessage("Scanning for your heart rate device. Please wait");
		progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progress.setIndeterminate(true);
		progress.show();

		Thread t = new Thread() {
			@Override
			public void run(){

				String deviceAddress=mBlueToothMethods.getHeartRateDeviceAddress();
				if(mBlueToothMethods.isDeviceHeartRateMonitor(deviceAddress)){
					HeartRateDevice hr= new HeartRateDevice(VitalSignsActivity.this,deviceAddress);
					CommonMethods.Log("About to call hr.getHeartRate() ");
					int heartRate=hr.getHeartRate();
					CommonMethods.Log("after call to hr.getHeartRate() heartRate = "+heartRate);
					if(heartRate>0){
						handlerScheduleMonitoringSessions.sendEmptyMessage(1);

					}else{
						handlerScheduleMonitoringSessions.sendEmptyMessage(0);
					}
				}else{
					handlerScheduleMonitoringSessions.sendEmptyMessage(0);
				}
				CommonMethods.Log("About to call progress.dismiss() ");
				progress.dismiss();

			}   
		};
		t.start();

	}

	private void stopApp() {
		CommonMethods.Log("stopApp called 1");
		try {
			flagShutDown=true;
			mCommonMethods.setFlagShutDown(flagShutDown);			 
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
		Runnable r1=new Runnable() {// because this is always called from a thread.
			@Override 
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
		};
		/*
		Handler buttonHandler = new Handler(this.getMainLooper());
		buttonHandler.post(r1);
		 */
		runOnUiThread(r1);

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
				@Override
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
			CommonMethods.Log("isHeartRateMonitorRunning = " + Monitors.Statuses.isPulseRateMonitorRunning);
			CommonMethods.Log("isBodyTemperatureMonitorRunning = " + Monitors.Statuses.isBodyTemperatureMonitorRunning);
			if (!mCommonMethods.isVitalSignsServiceRunning()
					&& !Monitors.Statuses.isPulseRateMonitorRunning
					&& !Monitors.Statuses.isBodyTemperatureMonitorRunning
					){
				allTasksNotRunning=true;
			}
			if(allTasksNotRunning){
				CommonMethods.Log("Hallelujah! All task have ended");
			}
			if(flagShutDown){
				CommonMethods.Log("flagShutDown is true");
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
/*
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

	*/
}
