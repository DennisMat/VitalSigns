
package com.dennis.vitalsigns;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import java.util.Date;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
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
	private Button buttonSettings;
	private LinearLayout LinearLayoutStartStop;
	private Button buttonScan;
	private Button buttonWarning;
	private TextView textViewHeartRate;

	private CommonMethods mCommonMethods = null;
	public static final String BUTTON_UPDATE = "buttonUpdate";
	public static final String HEART_RATE_DISPLAY_UPDATE = "heartRateDisplayUpdate";
	private Intent VitalSignsServiceIntent;
	private BlueToothMethods mBlueToothMethods=null;
	Preferences pref=null;

	private Handler handlerScheduleMonitoringSessions;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		try {

			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_main);

			CommonMethods.Log("in onCreate()");

			initializeVariables();
			initializeButtonListeners();
			allChecksPass();

			// get device id
			Context context = getApplicationContext();
			TelephonyManager tm = (TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE);
			String deviceID = tm.getDeviceId();


			updateButtonStatus();
			VitalSignsServiceIntent = new Intent(this, VitalSignsService.class);


		} catch (Exception e) {
			CommonMethods.Log("Exception " + e.getMessage());
		}

	}

	private BroadcastReceiver receiverButtonStatusUpdateEvent = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			CommonMethods.Log("receiverButtonStatusUpdateEvent onReceive() called" );
			updateButtonStatus();

		}
	};
	
	private BroadcastReceiver receiverHeartRateDisplayUpdateEvent = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			CommonMethods.Log("receiverHeartRateDisplayUpdateEvent onReceive() called" );
			
			Bundle extras = intent.getExtras();
			if (extras != null) {
			    int heartRate = extras.getInt("heartRate");
			    SimpleDateFormat timeFormat=new SimpleDateFormat("h:mm a");
			    textViewHeartRate.setText("Last received heart rate is " + heartRate + " beats per minute at "+
			    		timeFormat.format(new Date()) +". The next heart rate will be read at around "
			    		+ timeFormat.format(new Date(System.currentTimeMillis()+Preferences.hibernateTime*60*1000)) +".");
			    
			}			
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
		LocalBroadcastManager.getInstance(this).registerReceiver(receiverButtonStatusUpdateEvent, new IntentFilter(BUTTON_UPDATE));
		LocalBroadcastManager.getInstance(this).registerReceiver(receiverHeartRateDisplayUpdateEvent, new IntentFilter(HEART_RATE_DISPLAY_UPDATE));
		CommonMethods.Log("onResume called" );
		updateButtonStatus();
		allChecksPass();
	}

	@Override
	protected void onPause() {
		CommonMethods.Log("onPause called" );
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverButtonStatusUpdateEvent);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverHeartRateDisplayUpdateEvent);
		super.onPause();
	}

	private void initializeVariables() throws Exception {
		buttonStart = (Button) findViewById(R.id.buttonStart);
		buttonStop = (Button) findViewById(R.id.buttonStop);
		buttonSettings = (Button) findViewById(R.id.buttonSettings);
		LinearLayoutStartStop=(LinearLayout) findViewById(R.id.LinearLayoutStartStop);		
		buttonScan= (Button) findViewById(R.id.buttonScan);
		buttonWarning= (Button) findViewById(R.id.buttonWarning);
		textViewHeartRate=(TextView) findViewById(R.id.textViewHeartRate);
		
		mCommonMethods= new CommonMethods(this);		
		mBlueToothMethods= new BlueToothMethods(this);

		pref = new Preferences(this);
		pref.loadValuesFromStorage();

		handlerScheduleMonitoringSessions = new Handler(){
			@Override
			public void handleMessage(Message msg){
				if(msg.what == 0){
					flagShutDown=true;
					mCommonMethods.showMessage((VitalSignsActivity.this).getString(R.string.bluetooth_not_found));
				}else{
					mCommonMethods.scheduleRepeatingMonitoringSessions();// call the service right away.
					updateButtonStatus();
					flagShutDown=false;
					mCommonMethods.showMessage((VitalSignsActivity.this).getString(R.string.app_working));
					mCommonMethods.showNotification();
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

		buttonWarning.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mCommonMethods.showAlertDialog(getString(R.string.missing_phone_numbers));	
			}
		});


		buttonSettings.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent settingsActivity = new Intent(VitalSignsActivity.this,
						Preferences.class);
				startActivity(settingsActivity);
			}
		});

	}

	private boolean  allChecksPass(){
		if(checkForHeartRateDevice()){
			if(mBlueToothMethods.isBlueToothEnabled()){
				checkForPhoneNumbers();// this check needn't prevent the app form proceeding.
				return true;
			}else{
				mCommonMethods.showAlertDialogOnUiThread(getString(R.string.enable_bluetooth));				
			}
		}
		return false;
	}

	private boolean checkForHeartRateDevice(){		
		if(mBlueToothMethods.isHeartRateDeviceSet()){
			buttonScan.setVisibility(View.GONE);
			LinearLayoutStartStop.setVisibility(View.VISIBLE);
			return true;
		}else{
			buttonScan.setVisibility(View.VISIBLE);
			LinearLayoutStartStop.setVisibility(View.GONE);
			return false;
		}
	}

	private boolean checkForPhoneNumbers(){
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
		if(phoneNumbersExist){			
			buttonWarning.setVisibility(View.GONE);
		}else{
			buttonWarning.setVisibility(View.VISIBLE);
		}
		
		return phoneNumbersExist;
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
			 
				if(allChecksPass()){				
					scheduleMonitoringifHeartRateReceiving();
				}

			} else {
				mCommonMethods.showMessage(getString(R.string.app_expired));
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
		progress.setMessage((VitalSignsActivity.this).getString(R.string.scanning_heart_device));
		progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progress.setIndeterminate(true);
		progress.show();

		Thread t = new Thread() {
			@Override
			public void run(){

				String deviceAddress=mBlueToothMethods.getHeartRateDeviceAddress();					
					try {
						HeartRateDevice hr= new HeartRateDevice(VitalSignsActivity.this,deviceAddress);
						CommonMethods.Log("About to call hr.getHeartRate() ");
						int heartRate=hr.getHeartRate();
						CommonMethods.Log("after call to hr.getHeartRate() heartRate = "+heartRate);						
						if(heartRate>0){
							handlerScheduleMonitoringSessions.sendEmptyMessage(1);
						}else{
							handlerScheduleMonitoringSessions.sendEmptyMessage(0);
						}
					} catch (Exception e) {
						handlerScheduleMonitoringSessions.sendEmptyMessage(0);
						e.printStackTrace();
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
			mCommonMethods.cancelRepeatingMonitoringSessions();
			mCommonMethods.removeNotification();
			mCommonMethods.showToast(this.getString(R.string.app_stopped),Toast.LENGTH_LONG);

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
				mCommonMethods.removeNotification();
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
