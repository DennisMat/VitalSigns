package com.dennis.vitalsigns;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class CommonMethods {

	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
private int uniqueId = R.string.for_unique_number;

	public boolean isVitalSignsServiceRunning(Context context) {
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
		ToneGenerator tg = new ToneGenerator(
				android.media.AudioManager.STREAM_SYSTEM,
				ToneGenerator.MAX_VOLUME);
		try {
			tg.startTone(ToneGenerator.TONE_DTMF_A, durationMs);

		} catch (Exception e) {
			VitalSignsService.Log(e.getMessage());
		}
	}
	public void scheduleRepeatingMonitoringSessions(Context context) {
		scheduleRepeatingMonitoringSessions(context,0);
	}

	public void scheduleRepeatingMonitoringSessions(Context context, long triggerAtTime) {
		VitalSignsService.Log("in scheduleRepeatingMonitoringSessions");
		int hibernateTime=Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("key_hibernatetime",context.getString(R.string.pref_hibernatetime)));		
		long checkInterval = hibernateTime * 60 * 1000;//  this figure is in minutes so convert it into milliseconds
		if(triggerAtTime==0){//start right away, like yesterday :)
		// this is an arbitrary time just to make sure that the trigger time is well before the current time. This is when the alarm manager starts.
		 triggerAtTime = System.currentTimeMillis() - checkInterval;
		}
		
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
	
	

	public void cancelRepeatingMonitoringSessions(Context context) {
		VitalSignsService.Log("cancelRepeatingMonitoringSessions has been called");

		Intent moinitorIntent = new Intent(context,	MonitorSessionInitBroadcastReciever.class);
		PendingIntent monitorPendingIntent = PendingIntent.getBroadcast(context,0, moinitorIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager AlarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		AlarmManager.cancel(monitorPendingIntent);
	}

	
	/**
	 * show notification on task bar of the phone
	 */
	public void showNotification(Context context){
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
	/*
	 * Remove notification form task bar of the phone
	 */
	public void removeNotification(Context context){
		NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(uniqueId);
		Toast.makeText(context, "VitalSigns App has stopped. You must manually restart if you wnat the app to run again",
				Toast.LENGTH_SHORT).show();
		
	}
	
	public void playAudio(Context context){
		MediaPlayer mMediaPlayer = MediaPlayer.create(context, R.raw.double_beep);
		int numberOfBeeps=5;
		for (int i = 0; i < numberOfBeeps; i++) {
			mMediaPlayer.start();
			
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		mMediaPlayer.reset();//for whatver reason not calling gives the error:mediaplayer went away with unhandled events
		mMediaPlayer.release();
	}

}
