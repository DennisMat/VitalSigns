package com.dennis.vitalsigns;


import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
public class Monitors {

	Preferences pref;
	private Context context;
	private boolean[] beepHistory;
	private CommonMethods mCommonMethods = null;
	private int emergencylevelThreshold=0;
	
	
	public static class Statuses{
		public static boolean isHeartRateMonitorRunning=false;
		public static boolean isBodyTemperatureMonitorRunning=false;
	}

	public Monitors(Context context, Preferences pref) {
		this.context = context;
		this.pref = pref;
	}

	public void start() {
		try {
			VitalSignsActivity.flagShutDown=false;
			updateButtons();//The steps below may takes time so update button before this
			CommonMethods.Log("Monitors.start() started");
			initializeVariables();
			startMonitoringSession();
			mCommonMethods.showNotification(); 
			CommonMethods.aquirePartialWakeLock(context);
		} catch (Exception e) {
			VitalSignsActivity.flagShutDown=true;
			updateButtons();
			CommonMethods.Log("Exception: " + e.getMessage());
		}

	}
	
	/**It's between the startMonitoringSession() and stopMonitoringSession() that actual monitoring is done.
	 * Between the stopMonitoringSession() and startMonitoringSession() - sometimes hibernation is done to save on battery life.
	 * 
	 */
	public void startMonitoringSession(){
		if(VitalSignsActivity.flagShutDown){
			return;
		}
		// write code here to receive bluetooth 4.0 sgnals
		Statuses.isHeartRateMonitorRunning=true;
		PulseRateMonitor mBlueToothMonitor=new PulseRateMonitor(context);
		boolean emergencyStatusPulseRate=mBlueToothMonitor.getPersonEmergencyStatus();// this method may take time
		Statuses.isHeartRateMonitorRunning=false;

		Statuses.isBodyTemperatureMonitorRunning=true;//set true before reading values
		boolean emergencyStatusBodyTemp=false;//Todo later
		Statuses.isBodyTemperatureMonitorRunning=false;//set false before reading values

		boolean emergencyStatusBreathing=false;//Todo later
		int emergencylevel=0;
		
		if(emergencyStatusPulseRate){ 
			emergencylevel++;
		}

		if(emergencyStatusBodyTemp){
			emergencylevel++;
		}
		if(emergencyStatusBreathing){
			emergencylevel++;
		}

		if(emergencylevel>=emergencylevelThreshold){

			mCommonMethods.playBeep(100);
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
			mCommonMethods.showToast("Count down="+(pref.countDown-beepCount)+" . When the count down become zero your phone will start dialing");

			if (isNotify) {			
				mCommonMethods.playAudio();// We need a different sound. It should wake up a dead person if necessary :)
				CommonMethods.Log("before calling notifyPeople()");
				CommonMethods.Log("notifyPeople() commented - uncommentlater");
				//notifyPeople();//now call and sms people
				CommonMethods.Log("after calling notifyPeople()");			
				mCommonMethods.removeNotification();
				VitalSignsActivity.flagShutDown=true;
				updateButtons();			

				mCommonMethods.cancelRepeatingMonitoringSessions();
				stopMonitoringSession();
			} else {
				CommonMethods.Log("ContinueMonitoring called because no Vitalsigns above threshold level detected");
				continueMonitoring();
			}
		} else {
			if (pref.hibernateTime == 0) {
				CommonMethods.Log("continueMonitoring called because of 0 hibernate time");
				//clear all past beep history.
				for (int i = 0; i < beepHistory.length; i++) {
					beepHistory[i]=false;
				}
				continueMonitoring();
			}else{
				stopMonitoringSession();
				CommonMethods.Log("VitalSignsService will be called again by alarm manager");
			}

		}

	}



	private void setValuesForTesting() {
		
		pref.timeBetweenDialing=30;
		pref.hibernateTime=0;
		pref.countDown=4;
		pref.timeBetweenMonitoringSessions=10;
		for(int i=0;i<pref.arraySize;i++){

			pref.dialArray[i]=false;
			pref.SMSArray[i]=false;
		}
		pref.messageShowInPopup=true;
		pref.remoteLog=false;
		emergencylevelThreshold=1;
	}

	private void initializeVariables() throws Exception {

		mCommonMethods= new CommonMethods(context);

		pref.arraySize=3;
		pref.phoneNumberArray= new String[pref.arraySize];
		pref.dialArray= new boolean[pref.arraySize];
		pref.SMSArray= new boolean[pref.arraySize];
		emergencylevelThreshold=Integer.parseInt(context.getString(R.string.emergency_threshhold_level));
		pref.loadValuesFromStorage();// loads  historySize, countDown etc.
		setValuesForTesting();//overrrides some of the above variables.

		beepHistory = new boolean[pref.countDown];

		for (int i = 0; i < beepHistory.length; i++) {
			beepHistory[i] = false;
		}
		
		

	}

	private void stopMonitoringSession() {
		CommonMethods.Log(" in stopMonitoring()");
		try {

			CommonMethods.releasePartialWakeLock();

		} catch (Exception e) {
			CommonMethods.Log("Exception in stopMonitoring()" + e.getMessage());
		}
	}

	
	private void updateButtons(){
		//context.sendBroadcast(new Intent(VitalSignsActivity.BUTTON_UPDATE));
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(VitalSignsActivity.BUTTON_UPDATE));
		CommonMethods.Log("after sending the broadcast to update  buttons");

	}

	private void notifyPeople() {

		NotifyPeopleAsyncTask mNotifyPeopleAsyncTask = new NotifyPeopleAsyncTask();
		mNotifyPeopleAsyncTask.execute();


	}

	private void continueMonitoring() {
		if(VitalSignsActivity.flagShutDown){
			return;
		}
		try {
			Thread.sleep(pref.timeBetweenMonitoringSessions * 1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		startMonitoringSession();

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
					if(VitalSignsActivity.flagShutDown){
						return null;//abruptly ending dialing/sms because stop button was clicked.
					}
					if (pref.dialArray[i]) {
						phone.dialNumber(pref.phoneNumberArray[i]);
					}
					if(VitalSignsActivity.flagShutDown){
						return null;
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



}
