package com.dennis.vitalsigns;

import java.util.ArrayList;
import java.util.List;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import android.media.ToneGenerator;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.SharedPreferences;

import org.apache.http.NameValuePair;

import org.apache.http.client.entity.UrlEncodedFormEntity;

import org.apache.http.message.BasicNameValuePair;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;


public class VitalSignsService extends Service {

	private String phoneNumber;
	private String[] phoneNumberArray;
	private boolean[] dialArray;
	private boolean[] SMSArray;
	private int arraySize=0;
	private int timeBetweenDialing = 0;
	private boolean messageShowInPopup = false;
	private static boolean remoteLog = false;
	private static String logTag = "VitalSignsTag";
	public static PowerManager.WakeLock mWakeLock = null;

	private int statusThreshold = 0;
	private int deltaThreshold = 0;
	private int historySize = 0;
	private int countDown = 0; // Count down before dialing.
	private int timeBetweenMonitoringSessions = 0;// in seconds
	private int hibernateTime=0;
	private Context context = null;

	private int[] statusHistory;// stores the history in
														// ones and zeros. The
														// ones and zeros are
														// deduced from the
														// accelerometer
														// readings.
	private int[] deltaHistory;
	private boolean[] beepHistory;
	private int deltaIndexIncrement = 0;

	private Handler toastHandler;
	private String toastMessage = "";

	private float xacc = 0;
	private float yacc = 0;
	private float zacc = 0;

	private float xaccPrevious = 0;
	private float yaccPrevious = 0;
	private float zaccPrevious = 0;

	SensorManager mSensorManager = null;
	private SensorEventListener mEventListenerAccelerometer;
	
	ToneGenerator tg = null;

	public static VitalSignsActivity mainActivityReference;
	private boolean isSensorRegistered = false;


	// hooks main activity here
	public static void setMainActivity(VitalSignsActivity activity) {
		mainActivityReference = activity;
	}

	@Override
	public void onCreate() {
		try {
			super.onCreate();
			context = getApplicationContext();
			mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
			Log("Service started");
			initializeVariables();
			startAccMonitoring();
			(new CommonMethods()).showNotification(context); 
			aquirePartialWakeLock();
		} catch (Exception e) {
			resetButtons();
			Log("Exception: " + e.getMessage());
		}
	}

	private void setValuesForTesting() {
		statusThreshold = 200;
		deltaThreshold = 300;
		
		historySize=500;		
		timeBetweenDialing=30;
		hibernateTime=0;//in min
		countDown=5;
		timeBetweenMonitoringSessions=10;//in sec
		
		messageShowInPopup=true;
		remoteLog=false;

	}
	
	private void initializeVariables() throws Exception {
		initToastHandlers();
		initListener();
		arraySize=3;
		phoneNumberArray= new String[arraySize];
 		dialArray= new boolean[arraySize];
 		SMSArray= new boolean[arraySize];
		loadValuesFromStorage();// loads  historySize, countDown etc.
		//setValuesForTesting();//overrrides some of the above variables.
		 statusHistory = new int[historySize];
		 deltaHistory = new int[historySize];
 		beepHistory = new boolean[countDown];
 		
 		arraySize=3;
		for (int i = 0; i < deltaHistory.length; i++) {
			deltaHistory[i] = 0;
		}
		for (int i = 0; i < beepHistory.length; i++) {
			beepHistory[i] = false;
		}

	}
	


	private void loadValuesFromStorage() {
		SharedPreferences settings=PreferenceManager.getDefaultSharedPreferences(context);
		for(int i=0;i<arraySize;i++){
			phoneNumberArray[i]=settings.getString("key_ph"+i,Defaults.phoneNumberArray[i]);
			dialArray[i]=settings.getBoolean("key_dial"+i,Defaults.dialArray[i]);
			SMSArray[i]=settings.getBoolean("key_sms"+i,Defaults.SMSArray[i]);
		}

		historySize=Integer.parseInt(settings.getString("key_historysize",Defaults.historySize+""));
		deltaThreshold=Integer.parseInt(settings.getString("key_deltathreshold",Defaults.deltaThreshold+""));
		statusThreshold=Integer.parseInt(settings.getString("key_statusthreshold",Defaults.statusThreshold+""));
		timeBetweenDialing=Integer.parseInt(settings.getString("key_timebetweendialing",Defaults.timeBetweenDialing+""));
		hibernateTime=Integer.parseInt(settings.getString("key_hibernatetime",Defaults.hibernateTime+""));
		countDown=Integer.parseInt(settings.getString("key_countdown",Defaults.countDown+""));
		timeBetweenMonitoringSessions=Integer.parseInt(settings.getString("key_timebetweenmonitoring",Defaults.timeBetweenMonitoringSessions+""));
		
		messageShowInPopup=settings.getBoolean("key_showpopup", Defaults.messageShowInPopup);
		remoteLog=settings.getBoolean("key_remotelog", Defaults.remoteLog);

	}
	
	@Override
	public void onDestroy() {
		try {
			super.onDestroy();
			if (mainActivityReference != null) {
				// Log.d(getClass().getSimpleName(),
				// "VitalSignsService stopped");
			}
			
		} catch (Exception e) {
			releasePartialWakeLock();
		}
	}

	private void startAccMonitoring() {
		try {
			Log("in startMonitoringAccelerometer");
			if (!isSensorRegistered) {
				mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
				isSensorRegistered = mSensorManager.registerListener(
						mEventListenerAccelerometer, mSensorManager
								.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
						SensorManager.SENSOR_DELAY_FASTEST);
			}

		} catch (Exception e) {
			Log("Exception in startAccMonitoring" + e.getMessage());
		}
	}

	private void stopAccMonitoring() {
		Log(" in stopAccMonitoring()");
		try {
			if (isSensorRegistered) {
				mSensorManager.unregisterListener(mEventListenerAccelerometer);
				isSensorRegistered = false;
				java.util.Date CurrTime = new java.util.Date();
				// Log( "Sensor UnRegistered Time= " + CurrTime.getHours()+" : "
				// + CurrTime.getMinutes()+" : " +CurrTime.getSeconds());
			}

		} catch (Exception e) {
			Log("Exception in stopMonitoring()" + e.getMessage());
		}
	}

	/*
	 * shutting down the service
	 */
	private void shutdownService() {
		try {
			Log("shutdownService called");
			releasePartialWakeLock();
			stopAccMonitoring();
			if (isVitalSignsServiceRunning()) {
				stopSelf();
			}
		} catch (Exception e) {
		}

	}
	

	private void resetButtons(){
		Log("resetButtons called");
		if (VitalSignsActivity.buttonStatusUpdateHandler != null) {
			VitalSignsActivity.setStartButtonStatus(true);
			VitalSignsActivity.buttonStatusUpdateHandler.sendEmptyMessage(0);
		}
	}

	private void stopBeep() {
		try {
			if (tg != null) {
				tg.stopTone();
				tg.release();
				tg = null;
				// Log("stopTone called in stopBeep");
			}
		} catch (Exception e) {
			Log(e.getMessage());
		}
	}

	private boolean isVitalSignsServiceRunning() {
		ActivityManager manager = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
		boolean isRunning=false;
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if ("com.dennis.vitalsigns.VitalSignsService".equals(service.service.getClassName())) {				
				isRunning=true;
				break;
			}
		}
		return isRunning;
	}

	private void initToastHandlers() throws Exception {


		toastHandler = new Handler() {
			@Override
			public void dispatchMessage(Message msg) {
				super.dispatchMessage(msg);
				Log(toastMessage);
				Toast toast = Toast.makeText(context,
						toastMessage, Toast.LENGTH_SHORT);
				toast.show();

			}
		};

	}



	private void initListener() {
		mEventListenerAccelerometer = new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent event) {
				try {
					// Log("onSensorChanged: ", "onSensorChanged accessed");
					//Log("x: " + event.values[0] + ", y: " + event.values[1] + ", z: " + event.values[2]);

					if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
						// Log( "In onSensorChanged" + (new
						// java.util.Date()).getSeconds());

						xaccPrevious = xacc;
						yaccPrevious = yacc;
						zaccPrevious = zacc;
						xacc = event.values[0];
						yacc = event.values[1];
						zacc = event.values[2];

						if (deltaIndexIncrement < deltaHistory.length) {
							//Log("in deltaIndexIncrement ="	+ deltaIndexIncrement);
							deltaHistory[deltaIndexIncrement] = (int) ((Math
									.abs(xaccPrevious - xacc)
									+ Math.abs(yaccPrevious - yacc) + Math
									.abs(zaccPrevious - zacc)) * 1000);
							deltaIndexIncrement++;
						} else {
							deltaIndexIncrement=0;
							stopAccMonitoring();// stop reading
							if(!VitalSignsActivity.startButtonStatus){//process only if start button is in the pressed state						
								processDeltaValues();
							}
						}

					}

				} catch (Exception e) {
					Log(e.getMessage());
				}
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}
		};
	}

	/* accelerometer code ends */

	private void processDeltaValues() {
		Log("in processAccValues");
		// calculate status history
		for (int i = 0; i < statusHistory.length; i++) {
			//Log("deltaHistory[" + i + "]=" + deltaHistory[i]);
			if (deltaHistory[i] > deltaThreshold) {
				statusHistory[i] = 1;
			} else {
				statusHistory[i] = 0;
			}

		}

		int sumStatus = 0;

		// add up status history
		for (int i = 0; i < statusHistory.length; i++) {
			sumStatus += statusHistory[i];
		}
		showAverageDelta();
		if (sumStatus < statusThreshold) {
			Log("sumStatus is less than statusThreshold About to beep.\n sumStatus="
					+ sumStatus + " statusThreshold=" + statusThreshold);
			
			CommonMethods cm=new CommonMethods();
			cm.playBeep(100);
			// shift beep history values
			for (int i = 0; i < beepHistory.length; i++) {
				if ((i - 1) >= 0) {
					beepHistory[i - 1] = beepHistory[i];
				}
			}
			if(countDown>0){
			beepHistory[beepHistory.length - 1] = true;
			}

			boolean isNotify = true;
			for (int i = 0; i < beepHistory.length; i++) {
				Log("beepHistory[" + i + "]=" + beepHistory[i]);
			}
			int beepCount=0;
			for (int i = beepHistory.length-1; i >=0 ; i--) {			
				if (beepHistory[i] == false) {
					isNotify = false;
					break;
				}else{
					beepCount++;
				}
			}
			showToast("Count down="+(countDown-beepCount)+" . When the count down become zero your phone will start dialing");
			
			if (isNotify) {
				// notifyPeople();//now call and sms people
				cm.playAudio(context);// We need a different sound. It should wake up a dead person if necessary :)
				Log("notifyPeople() called");			
				cm.removeNotification(context);				
				resetButtons();
				(new CommonMethods()).cancelRepeatingMonitoringSessions(context);
				shutdownService();
			} else {
				Log("continueMonitoring called because of inactivity");
				continueMonitoring();
			}
		} else {//if sumStatus > statusThreshold i.e if alive
			if (hibernateTime == 0) {
				Log("continueMonitoring called because of 0 hibernate time");
				//clear all past beep history.
				for (int i = 0; i < beepHistory.length; i++) {
					beepHistory[i]=false;
				}
				Log("sumStatus="+ sumStatus + " statusThreshold=" + statusThreshold);
				continueMonitoring();
			}else{
				shutdownService();
				Log("service will be called again by alarm manager");
			}

		}

	}


	
	private void continueMonitoring() {

		try {
			Thread.sleep(timeBetweenMonitoringSessions * 1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		startAccMonitoring();

	}



	public void showToast(String msgstr) {
		toastMessage = msgstr;
		if (toastHandler != null) {
			toastHandler.sendEmptyMessage(0);
		}
	}

	public void showToast(String msgstr, boolean messageShowInPopup) {
		Log(msgstr);
		if(messageShowInPopup){
			toastMessage = msgstr;
			if (toastHandler != null) {
				toastHandler.sendEmptyMessage(0);
			}
		}
		
	}
	
	public void showAverageDelta() {
		StringBuilder sb = new StringBuilder();
		int averageDeltaHistory = 0;
		for (int i = 0; i < deltaHistory.length; i++) {
				averageDeltaHistory += deltaHistory[i];
		}
		averageDeltaHistory = averageDeltaHistory / deltaHistory.length;
		sb = sb.append("Average movement=" + averageDeltaHistory + " Threshold=" + deltaThreshold );
		showToast(sb.toString(),messageShowInPopup);
	}



	public boolean notifyPeople() {

		for (int i = 0; i < phoneNumberArray.length; i++) {
			try {
				if (dialArray[i]) {
					dialNumber(phoneNumberArray[i]);
				}
				if (SMSArray[i]) {
					SMS mSMS = new SMS(getApplicationContext());
					mSMS.sendSMS(phoneNumberArray[i], mSMS.getMessageForSMS());
				}
				if (!isVitalSignsServiceRunning()) { // stop notifying people if
														// service is stopped
					return false;
				}
				Thread.sleep(timeBetweenDialing * 1000); // seconds between
															// dialing
				if (!isVitalSignsServiceRunning()) {
					return false;
				}

			} catch (Exception e) { // stop notifying people if service is
									// stopped
				// TODO Auto-generated catch block
				Log("InterruptedException " + e.getMessage());
			}
		}

		return true;
	}

	public void dialNumber(String phno) {
		if (phno == null) {
			return;
		}
		if (phno.trim().compareTo("") == 0) {
			return;
		}

		try {
			showToast("Dialing: " + phoneNumber);
			// Log("Dialing: "+ PhoneNumber);
			Intent CallIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"
					+ phno));
			CallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			showToast("About to dial");
			startActivity(CallIntent);
			Log("Dialed number successfully");
		} catch (Exception e) {
			Log("Exception in DialNumber " + e.getMessage());

		}

	}

	private void aquirePartialWakeLock() throws Exception {
		if (mWakeLock == null) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, logTag);
			mWakeLock.acquire();
		}

	}

	public static void releasePartialWakeLock() {
		try {
			mWakeLock.release();
			mWakeLock = null;
		} catch (Exception e) {
		}

	}

	public static void aquirePartialWakeLock(Context context)
			throws Exception {
		if (mWakeLock == null) {
			PowerManager pm = (PowerManager) context
					.getSystemService(POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, logTag);
			mWakeLock.acquire();
		}

	}

	static public void Log(String logmessage) {		
		try {
			if (remoteLog) {
				HttpClient httpclient = new DefaultHttpClient();
				HttpPost httppost = new HttpPost(
						"http://photonshift.com/alert/Index.aspx");
				try {
					// Add your data
					List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
					nameValuePairs.add(new BasicNameValuePair("logmessage",
							logmessage));
					httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
					// Execute HTTP Post Request
					// HttpResponse response = httpclient.execute(httppost);
					httpclient.execute(httppost);
				} catch (Exception e) {
					Log.i(logTag, e.getMessage());
				}

			} else {
				Log.i(logTag, logmessage);
			}
		} catch (Exception e) {

		}

	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
