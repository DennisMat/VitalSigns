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
		public static boolean isPulseRateMonitorRunning=false;
		public static boolean isBodyTemperatureMonitorRunning=false;
	}

	public Monitors(Context context, Preferences pref) {
		this.context = context;
		this.pref = pref;
	}

	public void start() {		
		try {
			CommonMethods.aquirePartialWakeLock(context);
			VitalSignsActivity.flagShutDown=false;
			updateButtons();//The steps below may takes time so update button before this
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
			if (pref.hibernateTime == 0) {
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
				cal.add(Calendar.MINUTE,pref.hibernateTime);
				CommonMethods.Log("VitalSignsService will be called again by alarm manager at approximately " + cal.getTime());
			}

		}

	}



	private void setValuesForTesting() {
		
		pref.timeBetweenDialing=30;
		pref.hibernateTime=1;
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
		/*
		pref.arraySize=3;
		pref.phoneNumberArray= new String[pref.arraySize];
		pref.dialArray= new boolean[pref.arraySize];
		pref.SMSArray= new boolean[pref.arraySize];
		*/
		emergencylevelThreshold=Integer.parseInt(context.getString(R.string.emergency_threshhold_level));

		//setValuesForTesting();//overrrides some of the  variables in pref.

		beepHistory = new boolean[pref.countDown];

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
			CommonMethods.Log("sms for sending = " + phone.getMessageForSMS());
			/*we send the smses first before dialing. Dialling is  to be more error prone than sms.
			For example somebody might notice the phone trying to call and might hit the "hangup" button of the dialer.
			An sms to St. Peter at Pearly Gates is also a good idea*/
			for (int i = 0; i < pref.phoneNumberArray.length; i++) {
				try {

					if (pref.SMSArray[i]) {
						phone.sendSMS(pref.phoneNumberArray[i], phone.getMessageForSMS());
					}

				} catch (Exception e) {
					CommonMethods.Log("Exception " + e.getMessage());
				}
			}
			
			//By now the horse has left the stable (via sms)- we can start dialling leisurely.
			for (int i = 0; i < pref.phoneNumberArray.length; i++) {
				try {

					if (pref.dialArray[i]) {
						phone.dialNumber(pref.phoneNumberArray[i]);
					}

					//TODO:put some kind of cancel dialing feature here.
					Thread.sleep(pref.timeBetweenDialing * 1000);

				} catch (Exception e) {
					CommonMethods.Log("Exception " + e.getMessage());
				}
			}
			CommonMethods.Log("done dialing and sending smses");
			// this notification takes time because of pref.timeBetweenDialing and is probably not evenneeded.
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
