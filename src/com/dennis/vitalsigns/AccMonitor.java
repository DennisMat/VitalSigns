package com.dennis.vitalsigns;



import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.widget.Toast;

public class AccMonitor {

	

	/**
	 * stores the history in ones and zeros. The ones and zeros are  deduced from the  accelerometer  readings.
	 */
	/*
	public int[] statusHistory;
	public int[] deltaHistory;
	public boolean[] beepHistory;
	public int deltaIndexIncrement = 0;

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
	
	Preferences pref;
	private Context context;
	
	public AccMonitor(Context context){
		this.context=context;
	}
	
	/**
	 * Called from VitalSignsService service.
	 *
	public void start() {
		try {
			mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
			CommonMethods.Log("Service started");
			initializeVariables();
			startAccMonitoring();
			(new CommonMethods()).showNotification(context); 
			aquirePartialWakeLock();
		} catch (Exception e) {
			resetButtons();
			CommonMethods.Log("Exception: " + e.getMessage());
		}
	}
	
	
	
	private void setValuesForTesting() {
		pref.statusThreshold = 200;
		pref.deltaThreshold = 300;

		pref.historySize=500;		
		pref.timeBetweenDialing=30;
		pref.hibernateTime=0;//in min
		pref.countDown=5;
		pref.timeBetweenMonitoringSessions=10;//in sec

		pref.messageShowInPopup=true;
		pref.remoteLog=false;

	}

	private void initializeVariables() throws Exception {
		initListener();
		pref = new Preferences();
		
		pref.arraySize=3;
		pref.phoneNumberArray= new String[pref.arraySize];
		pref.dialArray= new boolean[pref.arraySize];
		pref.SMSArray= new boolean[pref.arraySize];
		pref.loadValuesFromStorage();// loads  historySize, countDown etc.
		//setValuesForTesting();//overrrides some of the above variables.
		statusHistory = new int[pref.historySize];
		deltaHistory = new int[pref.historySize];
		beepHistory = new boolean[pref.countDown];


		for (int i = 0; i < deltaHistory.length; i++) {
			deltaHistory[i] = 0;
		}
		for (int i = 0; i < beepHistory.length; i++) {
			beepHistory[i] = false;
		}

	}



	private void startAccMonitoring() {
		try {
			CommonMethods.Log("in startMonitoringAccelerometer");
			if (!isSensorRegistered) {
				mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
				isSensorRegistered = mSensorManager.registerListener(
						mEventListenerAccelerometer, mSensorManager
						.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
						SensorManager.SENSOR_DELAY_FASTEST);
			}

		} catch (Exception e) {
			CommonMethods.Log("Exception in startAccMonitoring" + e.getMessage());
		}
	}

	/**
	 * called by the sensor listener and shutdownVitalSignsService()
	 *
	private void stopAccMonitoring() {
		CommonMethods.Log(" in stopAccMonitoring()");
		try {
			if (isSensorRegistered) {
				mSensorManager.unregisterListener(mEventListenerAccelerometer);
				isSensorRegistered = false;
				java.util.Date CurrTime = new java.util.Date();
				// Log( "Sensor UnRegistered Time= " + CurrTime.getHours()+" : "
				// + CurrTime.getMinutes()+" : " +CurrTime.getSeconds());
			}

		} catch (Exception e) {
			CommonMethods.Log("Exception in stopMonitoring()" + e.getMessage());
		}
	}

	/*
	 * shutting down the service
	 *
	private void shutdownVitalSignsService() {
		try {
			CommonMethods.Log("shutdownService called");
			CommonMethods.releasePartialWakeLock();
			stopAccMonitoring();

		} catch (Exception e) {
		}

	}



	private void notifyPeople() {

		NotifyPeopleAsyncTask mNotifyPeopleAsyncTask = new NotifyPeopleAsyncTask();
		mNotifyPeopleAsyncTask.execute();


	}


	private class NotifyPeopleAsyncTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute(){

		}

		@Override
		protected Void doInBackground(Void... args) {	
			Phone phone = new Phone(context);
			for (int i = 0; i < pref.phoneNumberArray.length; i++) {
				try {
					if (pref.dialArray[i]) {
						phone.dialNumber(pref.phoneNumberArray[i]);
					}
					if (pref.SMSArray[i]) {
						phone.sendSMS(pref.phoneNumberArray[i], phone.getMessageForSMS());
					}
					//put some kind of cancel dialing feature.
					//Thread.sleep(timeBetweenDialing * 1000); // seconds between dialing

				} catch (Exception e) {
					// TODO Auto-generated catch block
					CommonMethods.Log("InterruptedException " + e.getMessage());
				}
			}

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



	private void aquirePartialWakeLock() throws Exception {
		if (CommonMethods.mWakeLock == null) {
			PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
			CommonMethods.mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, pref.logTag);
			CommonMethods.mWakeLock.acquire();
		}

	}



	

	private void resetButtons(){
		CommonMethods.Log("resetButtons called");
		VitalSignsActivity.setStartButtonPressed(true);	
		context.sendBroadcast(new Intent(VitalSignsActivity.BUTTON_UPDATE));		
	}

	private void stopBeep() {
		try {
			if (tg != null) {
				tg.stopTone();
				tg.release();
				tg = null;
				// CommonMethods.Log("stopTone called in stopBeep");
			}
		} catch (Exception e) {
			CommonMethods.Log(e.getMessage());
		}
	}


	private void initListener() {
		mEventListenerAccelerometer = new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent event) {
				try {
					// CommonMethods.Log("onSensorChanged: ", "onSensorChanged accessed");
					//CommonMethods.Log("x: " + event.values[0] + ", y: " + event.values[1] + ", z: " + event.values[2]);

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
							//CommonMethods.Log("in deltaIndexIncrement ="	+ deltaIndexIncrement);
							deltaHistory[deltaIndexIncrement] = (int) ((Math
									.abs(xaccPrevious - xacc)
									+ Math.abs(yaccPrevious - yacc) + Math
									.abs(zaccPrevious - zacc)) * 1000);
							deltaIndexIncrement++;
						} else {
							deltaIndexIncrement=0;
							stopAccMonitoring();// stop reading
							if(!VitalSignsActivity.isStartButtonEnabled()){//process only if start button is in the pressed state						
								processDeltaValues();
							}
						}

					}

				} catch (Exception e) {
					CommonMethods.Log(e.getMessage());
				}
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}
		};
	}

	/* accelerometer code ends 

	private void processDeltaValues() {
		CommonMethods.Log("in processAccValues");
		// calculate status history
		for (int i = 0; i < statusHistory.length; i++) {
			//CommonMethods.Log("deltaHistory[" + i + "]=" + deltaHistory[i]);
			if (deltaHistory[i] > pref.deltaThreshold) {
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
		if (sumStatus < pref.statusThreshold) {
			CommonMethods.Log("sumStatus is less than statusThreshold About to beep.\n sumStatus="
					+ sumStatus + " statusThreshold=" + pref.statusThreshold);

			CommonMethods cm=new CommonMethods();
			cm.playBeep(100);
			// shift beep history values
			for (int i = 0; i < beepHistory.length; i++) {
				if ((i - 1) >= 0) {
					beepHistory[i - 1] = beepHistory[i];
				}
			}
			if(pref.countDown>0){
				beepHistory[beepHistory.length - 1] = true;
			}

			boolean isNotify = true;
			for (int i = 0; i < beepHistory.length; i++) {
				CommonMethods.Log("beepHistory[" + i + "]=" + beepHistory[i]);
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
			showToast("Count down="+(pref.countDown-beepCount)+" . When the count down become zero your phone will start dialing");

			if (isNotify) {			
				cm.playAudio(context);// We need a different sound. It should wake up a dead person if necessary :)
				CommonMethods.Log("before calling notifyPeople()");
				//notifyPeople() takes a lot of time and the screen is totally unresponsive when notifyPeople()
				notifyPeople();//now call and sms people
				CommonMethods.Log("after calling notifyPeople()");			
				cm.removeNotification(context);				
				resetButtons();			
				
				(new CommonMethods()).cancelRepeatingMonitoringSessions(context);
				shutdownVitalSignsService();
			} else {
				CommonMethods.Log("continueMonitoring called because of inactivity");
				continueMonitoring();
			}
		} else {//if sumStatus > statusThreshold i.e if alive
			if (pref.hibernateTime == 0) {
				CommonMethods.Log("continueMonitoring called because of 0 hibernate time");
				//clear all past beep history.
				for (int i = 0; i < beepHistory.length; i++) {
					beepHistory[i]=false;
				}
				CommonMethods.Log("sumStatus="+ sumStatus + " statusThreshold=" + pref.statusThreshold);
				continueMonitoring();
			}else{
				shutdownVitalSignsService();
				CommonMethods.Log("service will be called again by alarm manager");
			}

		}

	}



	private void continueMonitoring() {

		try {
			Thread.sleep(pref.timeBetweenMonitoringSessions * 1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		startAccMonitoring();

	}



	public void showToast(String msgstr) {
		Toast.makeText(context,msgstr,	Toast.LENGTH_LONG).show();
	}

	public void showToast(String msgstr, boolean messageShowInPopup) {
		CommonMethods.Log(msgstr);
		if(messageShowInPopup){
			showToast(msgstr);
		}

	}

	public void showAverageDelta() {
		StringBuilder sb = new StringBuilder();
		int averageDeltaHistory = 0;
		for (int i = 0; i < deltaHistory.length; i++) {
			averageDeltaHistory += deltaHistory[i];
		}
		averageDeltaHistory = averageDeltaHistory / deltaHistory.length;
		sb = sb.append("Average movement=" + averageDeltaHistory + " Threshold=" + pref.deltaThreshold );
		showToast(sb.toString(),pref.messageShowInPopup);
	}

*/
	
}
