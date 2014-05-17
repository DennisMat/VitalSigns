package com.dennis.vitalsigns;

import java.util.Calendar;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

public class Monitors {

	Preferences pref;
	private Context context;
	private boolean[] beepHistory;
	private CommonMethods mCommonMethods = null;
	private int emergencylevelThreshold=0;
	
	public static class Statuses{
		public volatile static boolean isPulseRateMonitorRunning=false;
		public volatile static boolean isBodyTemperatureMonitorRunning=false;
	}

	public Monitors(Context context, Preferences pref) {
		this.context = context;
		this.pref = pref;
	}

	public void start() {		
		try {
			CommonMethods.aquirePartialWakeLock(context);
			VitalSignsActivity.flagShutDown=false;
			updateButtons();//The steps below may take time so update buttons before this
			CommonMethods.Log("Monitors.start() started");
			initializeVariables();
			startMonitoringSession();			 
		} catch (Exception e) {
			VitalSignsActivity.flagShutDown=true;
			mCommonMethods.setFlagShutDown(VitalSignsActivity.flagShutDown);	
			updateButtons();
			CommonMethods.Log("Exception: " + e.getMessage());
		}

	}
	
	/**It's between the startMonitoringSession() and wrapUpMonitoringSession() that actual monitoring is done.
	 * After the  the wrapUpMonitoringSession() - there may be a hibernation time before startMonitoringSession() is called again.
	 *  This hibernation time will save on battery life. The phone is woken up from the hibernation state by the alarm manager.
	 * 
	 */
	public void startMonitoringSession(){
		if(VitalSignsActivity.flagShutDown){
			return;
		}
		BlueToothMethods mBlueToothMethods= new BlueToothMethods(context);
		boolean emergencyStatusPulseRate=false;
		
		Statuses.isPulseRateMonitorRunning=true;
		if(mBlueToothMethods.isHeartRateDeviceSet()){
			HeartRateDevice mHeartRateDevice=new HeartRateDevice(context,mBlueToothMethods.getHeartRateDeviceAddress());
			emergencyStatusPulseRate=mHeartRateDevice.getPersonHeartRateEmergencyStatus();// this method may take time
		}
		Statuses.isPulseRateMonitorRunning=false;

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

			mCommonMethods.playSounds();
			// shift beep history values
			for (int i = 0; i < beepHistory.length; i++) {
				if ((i - 1) >= 0) {
					beepHistory[i - 1] = beepHistory[i];
				}
			}
			if(Preferences.countDown>0){
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
			mCommonMethods.showToast("Count down="+(Preferences.countDown-beepCount)+" . When the count down become zero your phone will start dialing");

			if (isNotify) {			
				mCommonMethods.playSoundBeforeAlertsAreSent();// We need a different sound. It should wake up a dead person if necessary :)
				CommonMethods.Log("before calling notifyPeople()");
				notifyPeople();//now call and sms people
				CommonMethods.Log("after calling notifyPeople()");			
				
				VitalSignsActivity.flagShutDown=true;
				mCommonMethods.setFlagShutDown(VitalSignsActivity.flagShutDown);
				updateButtons();			

				mCommonMethods.cancelRepeatingMonitoringSessions();
				
				wrapUpMonitoringSession();
				mCommonMethods.removeNotification();
			} else {
				CommonMethods.Log("ContinueMonitoring called because no Vitalsigns above threshold level detected");
				continueMonitoring();
			}
		} else {
			if (Preferences.hibernateTime == 0) {
				CommonMethods.Log("continueMonitoring called because of 0 hibernate time");
				//clear all past beep history.
				for (int i = 0; i < beepHistory.length; i++) {
					beepHistory[i]=false;
				}
				continueMonitoring();
			}else{
				wrapUpMonitoringSession();
				//SimpleDateFormat timingFormat = new SimpleDateFormat("h:mm a");
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.MINUTE,Preferences.hibernateTime);
				CommonMethods.Log("VitalSignsService will be called again by alarm manager at approximately " + cal.getTime());
			}

		}

	}



	private void setValuesForTesting() {
		
		Preferences.timeBetweenDialing=30;
		Preferences.hibernateTime=1;
		Preferences.countDown=4;
		Preferences.timeBetweenMonitoringSessions=10;
		for(int i=0;i<Preferences.arraySize;i++){

			Preferences.dialArray[i]=false;
			pref.SMSArray[i]=false;
		}
		Preferences.messageShowInPopup=true;
		Preferences.remoteLog=false;
		emergencylevelThreshold=1;
	}

	private void initializeVariables() throws Exception {

		mCommonMethods= new CommonMethods(context);
		/*
		pref.arraySize=3;
		pref.phoneNumberArray= new String[pref.arraySize];
		pref.dialArray= new boolean[pref.arraySize];
		pref.SMSArray= new boolean[pref.arraySize];
		*/
		emergencylevelThreshold=Integer.parseInt(context.getString(R.string.emergency_threshhold_level));

		//setValuesForTesting();//overrrides some of the  variables in pref.

		beepHistory = new boolean[Preferences.countDown];

		for (int i = 0; i < beepHistory.length; i++) {
			beepHistory[i] = false;
		}
		
		

	}

	private void wrapUpMonitoringSession() {
		CommonMethods.Log(" in stopMonitoring()");
		try {
			CommonMethods.releasePartialWakeLock();
		} catch (Exception e) {
			CommonMethods.Log("Exception in stopMonitoring()" + e.getMessage());
		}
	}

	
	private void updateButtons(){
		LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(VitalSignsActivity.BUTTON_UPDATE));
		CommonMethods.Log("after sending the broadcast to update  buttons");

	}

	private void notifyPeople() {

		NotifyPeopleAsyncTask mNotifyPeopleAsyncTask = new NotifyPeopleAsyncTask();
		mNotifyPeopleAsyncTask.execute();


	}

	private void continueMonitoring() {
		CommonMethods.Log("in continueMonitoring() 1");
		if(VitalSignsActivity.flagShutDown){
			CommonMethods.Log("in continueMonitoring() 2");
			mCommonMethods.removeNotification();
			return;
		}
		try {
			CommonMethods.Log("Time between monitoring sessions =" + pref.timeBetweenMonitoringSessions + " seconds.\n "
					+ "This is non-zero even for a zero hibernate time.");
			Thread.sleep(Preferences.timeBetweenMonitoringSessions * 1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		startMonitoringSession();

	}






	private class NotifyPeopleAsyncTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute(){
			try {
				CommonMethods.aquirePartialWakeLock(context);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		protected Void doInBackground(Void... args) {	
			Phone phone = new Phone(context);
			CommonMethods.Log("common sms for sending = " + phone.getMessageForSMS());
			CommonMethods.Log("additional sms 1 for sending = " + phone.getMessageAdditionalForSMS(0));
			CommonMethods.Log("additional sms 2 for sending = " + phone.getMessageAdditionalForSMS(1));
			CommonMethods.Log("additional sms 3 for sending = " + phone.getMessageAdditionalForSMS(2));
			/*we send the smses first before dialling. Dialling is  to be more error prone than sms.
			For example somebody might notice the phone trying to call and might hit the "hangup" button of the dialler.
			An sms to St. Peter at Pearly Gates is also a good idea*/
			for (int i = 0; i < Preferences.phoneNumberArray.length; i++) {
				try {

					if (Preferences.SMSArray[i]) {
						phone.sendSMS(Preferences.phoneNumberArray[i], phone.getMessageForSMS());
						phone.sendSMS(Preferences.phoneNumberArray[i], phone.getMessageAdditionalForSMS(i));
					}

				} catch (Exception e) {
					CommonMethods.Log("Exception " + e.getMessage());
				}
			}
			
			//By now the horse has left the stable (via sms)- we can start dialling leisurely.
			for (int i = 0; i < Preferences.phoneNumberArray.length; i++) {
				try {

					if (Preferences.dialArray[i]) {
						phone.dialNumber(Preferences.phoneNumberArray[i]);
					}

					//TODO:put some kind of cancel dialing feature here.
					Thread.sleep(Preferences.timeBetweenDialing * 1000);

				} catch (Exception e) {
					CommonMethods.Log("Exception " + e.getMessage());
				}
			}
			CommonMethods.Log("done dialing and sending smses");
			// this notification takes time because of pref.timeBetweenDialing and is probably not even needed.
			mCommonMethods.showToast(context.getString(R.string.app_stopped),Toast.LENGTH_LONG);

			return null;
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			//do stuff here
			super.onProgressUpdate(values);// this line is always the last
		}

		@Override
		protected void onPostExecute(Void result) {
			CommonMethods.releasePartialWakeLock();
		}

	}



}
