package com.dennis.vitalsigns;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class CommonMethods {

	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	private int uniqueId = R.string.for_unique_number;
	public static PowerManager.WakeLock mWakeLock = null;
	private Context context;

	public CommonMethods(Context context) {
		this.context = context;
	}


	public boolean isVitalSignsServiceRunning() {
		ActivityManager manager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
		boolean isRunning=false;
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if ("com.dennis.vitalsigns.VitalSignsService".equals(service.service.getClassName())) {				
				isRunning=true;
				break;
			}
		}
		return isRunning;
	}

	public void playBeep(int durationMs) {
		try {
			ToneGenerator tg = new ToneGenerator(
					android.media.AudioManager.STREAM_SYSTEM,
					ToneGenerator.MAX_VOLUME);

			tg.startTone(ToneGenerator.TONE_DTMF_A, durationMs);

		} catch (Exception e) {
			CommonMethods.Log(e.getMessage());
		}
	}
	public void scheduleRepeatingMonitoringSessions() {
		scheduleRepeatingMonitoringSessions(0);
	}

	public void scheduleRepeatingMonitoringSessions(long triggerAtTime) {
		
		int hibernateTime=Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("key_hibernatetime",context.getString(R.string.pref_hibernatetime)));
		//hibernateTime=1;//for testing only.
		long checkInterval = hibernateTime * 60 * 1000;//  this figure is in minutes so convert it into milliseconds
		if(triggerAtTime==0){//start right away, like yesterday :)
			triggerAtTime = System.currentTimeMillis();
		}
		
		//for logging
		SimpleDateFormat timingFormat = new SimpleDateFormat("h:mm a");
		CommonMethods.Log("in scheduleRepeatingMonitoringSessions. The VitalSignsService will be started at " + timingFormat.format(new Date(triggerAtTime)));
		
		AlarmManager mAlarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		Intent monitorIntent = new Intent(context,
				MonitorSessionInitBroadcastReciever.class);
		PendingIntent monitorPendingIntent = PendingIntent.getBroadcast(context,
				0, monitorIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		if (checkInterval == 0) {
			mAlarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtTime,
					monitorPendingIntent); // Call this again when the monitoring is done so we have it repeating without overlapps
		} else {
			mAlarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
					triggerAtTime, checkInterval, monitorPendingIntent);
		}

	}



	public void cancelRepeatingMonitoringSessions() {
		CommonMethods.Log("cancelRepeatingMonitoringSessions has been called");

		Intent moinitorIntent = new Intent(context,	MonitorSessionInitBroadcastReciever.class);
		PendingIntent monitorPendingIntent = PendingIntent.getBroadcast(context,0, moinitorIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager AlarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		AlarmManager.cancel(monitorPendingIntent);
	}

	
	//TODO: Dennis: The notifications are not working properly. Fix them up later.
	/**
	 * show notification on task bar of the phone
	 */
	public void showNotification(){
		NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.icon, "VitalSigns App has started",
				System.currentTimeMillis());
		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				new Intent(context, VitalSignsActivity.class), 0);
		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(context,"VitalSigns App running", "VitalSigns App running", contentIntent);
		// Send the notification.
		mNotificationManager.notify(uniqueId, notification);

	}
	
	/**
	 * Remove notification from task bar of the phone
	 */
	public void removeNotification(){
		NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(uniqueId);
		String msg="VitalSigns App has stopped. You must manually restart if you wnat the app to run again";
		showToast(msg,Toast.LENGTH_SHORT);

	}

	public void showToast(final String msgstr,final int toastLength) {
		Handler toastHandler = new Handler(context.getMainLooper());
		toastHandler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(context,msgstr,toastLength).show();
			}
		});
	}
	
	public void showToast(final int msgstr,final int toastLength) {
		Handler toastHandler = new Handler(context.getMainLooper());
		toastHandler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(context,msgstr,toastLength).show();
			}
		});
	}

	public void showToast(String msgstr) {
		showToast(msgstr,Toast.LENGTH_LONG);
	}
	public void playAudio(){
		MediaPlayer mMediaPlayer = MediaPlayer.create(context, R.raw.double_beep);
		int numberOfBeeps=5;
		for (int i = 0; i < numberOfBeeps; i++) {
			if(VitalSignsActivity.flagShutDown){
				return;
			}
			mMediaPlayer.start();

			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		mMediaPlayer.reset();//for whatever reason not calling gives the error:mediaplayer went away with unhandled events
		mMediaPlayer.release();
	}

	public static void releasePartialWakeLock() {
		try {
			mWakeLock.release();
			mWakeLock = null;
			CommonMethods.Log("WakeLock released");
		} catch (Exception e) {
		}

	}

	public static void aquirePartialWakeLock(Context context)
			throws Exception {
		if (mWakeLock == null) {
			PowerManager pm = (PowerManager) context
					.getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Preferences.logTag);
			mWakeLock.acquire();
			CommonMethods.Log("WakeLock aquired");
		}

	}
	/**Called in the following situations: 
	 * <br/>-- pressing the start(flagShutDown=false) or stop button(flagShutDown=true)
	 * <br/>-- after the alert is sent out. (flagShutDown=true)
	 * 
	 * @param flagShutDown
	 * @throws Exception
	 */
	public void setFlagShutDown(boolean flagShutDown){
		 try {
			SharedPreferences settings=PreferenceManager.getDefaultSharedPreferences(context);
			 SharedPreferences.Editor editor = settings.edit();
			 editor.putBoolean("flagShutDown", flagShutDown);
			 editor.putString("ddffcc", "str1");
			 CommonMethods.Log("Value is set");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public boolean getFlagShutDown()
			throws Exception {
		SharedPreferences settings=PreferenceManager.getDefaultSharedPreferences(context);
		boolean flagShutDown=settings.getBoolean("flagShutDown", false);
		CommonMethods.Log("Value of ddffcc is" + settings.getString("ddffcc", "wrong val"));
		return flagShutDown;

	}
	
	static public void Log(String logmessage) {		
		try {
			if (Preferences.remoteLog) {
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
					Log.i(Preferences.logTag, e.getMessage());
				}

			} else {
				Log.i(Preferences.logTag, logmessage);
			}
		} catch (Exception e) {

		}

	}

}
