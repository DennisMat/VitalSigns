package com.dennis.vitalsigns;




import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.app.ActivityManager.RunningServiceInfo;

/*This class is used to start the monitoring. 
 * It's always called by the Alarm Manager - never directly by the Start button.
 */
public class VitalSignsService extends Service {



	private static boolean remoteLog = false;


	@Override
	public void onCreate() {
		try {
			super.onCreate();
			CommonMethods.Log("in VitalSignsService.onCreate()");
			(new Preferences(this)).loadValuesFromStorage();
			BackgroundAsyncTask mBackgroundAsyncTask = new BackgroundAsyncTask(this);
			mBackgroundAsyncTask.execute();	
		} catch (Exception e) {
			CommonMethods.Log("Exception: " + e.getMessage());
		}
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		return super.onStartCommand(intent, flags, startId);
	}


	private class BackgroundAsyncTask extends AsyncTask<Void, Void, Void> {

		private Context mContext;
		Preferences pref=null; 
		public BackgroundAsyncTask(Context context) {
			mContext = context;
		} 

		@Override
		protected void onPreExecute(){
			/* pref is initiated here because it's a UI operation and will 
			 * throw an exception in doInBackground
			 */
			pref=new Preferences(mContext);
		}

		@Override
		protected Void doInBackground(Void... args) {
			CommonMethods mCommonMethods= new CommonMethods(mContext);
			mCommonMethods.updateLatLong();
			Monitors mMonitors=new Monitors(mContext,pref);
			mMonitors.start();//this step takes time
			CommonMethods.Log("MonitorAsyncTask.doInBackground() completed");
			return null;		
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			//do stuff here
			super.onProgressUpdate(values);// this line is always the last
		}

		@Override
		protected void onPostExecute(Void result) {
			if (isVitalSignsServiceRunning()) {// this check is redundant
				stopSelf();
				CommonMethods.Log("VitalSignsService has shut down");
			}
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


	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}






}
