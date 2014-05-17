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
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.*;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
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

	public void playSounds(){
		/*Mutiple sounds are played over here because the volume of each sound 
		is set differently on the phone. Some of them sound may be muted.
		We have to make sure that something is heard.
		*/
		playSound1(100);
		playSound2();
		playSound3();
	}
	
	/**
	 * This is a beep
	 * @param durationMs
	 */
	public void playSound1(int durationMs) {
		CommonMethods.Log("playSound1(dtmf tone)" );
		try {
			ToneGenerator tg = new ToneGenerator(
					android.media.AudioManager.STREAM_SYSTEM,
					ToneGenerator.MAX_VOLUME);

			tg.startTone(ToneGenerator.TONE_DTMF_A, durationMs);

		} catch (Exception e) {
			CommonMethods.Log(e.getMessage());
		}
	}
	
	public void playSound2(){
		CommonMethods.Log("playSound2A(double beep)" );
		MediaPlayer mMediaPlayer = MediaPlayer.create(context, R.raw.double_beep);
		mMediaPlayer.setVolume(1.0f, 1.0f);

			mMediaPlayer.start();

			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		mMediaPlayer.reset();//for whatever reason not calling gives the error:mediaplayer went away with unhandled events
		mMediaPlayer.release();
	}
	
	/**
	 * Same as about but 5 of them instead of 1
	 */
	public void playSound2A(){
		CommonMethods.Log("playSound2A(double beep)" );
		MediaPlayer mMediaPlayer = MediaPlayer.create(context, R.raw.double_beep);
		mMediaPlayer.setVolume(1.0f, 1.0f);
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
	

	public void playSound3() {
		try {
			CommonMethods.Log("playSound3(SMS notification sound)" );
			/*
	        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
	        MediaPlayer mMediaPlayer = new MediaPlayer();
	        mMediaPlayer.setDataSource(context, soundUri);
	        final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
	        if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
	            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
	            mMediaPlayer.setLooping(false);
	            mMediaPlayer.prepare();
	            mMediaPlayer.start();
	        }
	        */
			/*
		      Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); 
		        Ringtone r = RingtoneManager.getRingtone(context, soundUri);
		        r.play();
		        
		        */
			Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			MediaPlayer mMediaPlayer= MediaPlayer.create(context, alert); 
			mMediaPlayer.setVolume(1.0f, 1.0f);
			mMediaPlayer.start();
			mMediaPlayer.setOnCompletionListener(new OnCompletionListener(){
			@Override
			public void onCompletion(MediaPlayer mp){
				mp.release();
			}
			});

		} catch (Exception e) {
			CommonMethods.Log(e.getMessage());
		}
	}
	
	public void playSoundBeforeAlertsAreSent(){
		playSound2A();
	}
	
	public void scheduleRepeatingMonitoringSessions() {
		showNotification();// Dennis: This is the best place to show the notification
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
		SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a");
		CommonMethods.Log("in scheduleRepeatingMonitoringSessions. The VitalSignsService will be started at " + timeFormat.format(new Date(triggerAtTime)));
		
		AlarmManager mAlarmManager = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		Intent monitorIntent = new Intent(context,
				MonitorSessionInitBroadcastReceiver.class);
		PendingIntent monitorPendingIntent = PendingIntent.getBroadcast(context,
				0, monitorIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		if (checkInterval == 0) {
			mAlarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtTime,
					monitorPendingIntent); // Call this again when the monitoring is done so we have it repeating without overlap
		} else {
			mAlarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
					triggerAtTime, checkInterval, monitorPendingIntent);
		}

	}

	public void cancelRepeatingMonitoringSessions() {
		CommonMethods.Log("cancelRepeatingMonitoringSessions has been called");
		removeNotification();// Dennis: This is the best place to cancel the notification
		
		Intent moinitorIntent = new Intent(context,	MonitorSessionInitBroadcastReceiver.class);
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
		CommonMethods.Log("in showNotification");	
		Intent notificationIntent = new Intent(context, VitalSignsActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(context,
		        0, notificationIntent,
		        PendingIntent.FLAG_CANCEL_CURRENT);

		NotificationManager nm = (NotificationManager) context
		        .getSystemService(Context.NOTIFICATION_SERVICE);

		Resources res = context.getResources();
		Notification.Builder builder = new Notification.Builder(context);

		builder.setContentIntent(contentIntent)
		            .setSmallIcon(R.drawable.icon)
		            .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.icon))
		            .setTicker("VitalSigns App has started")
		            .setWhen(System.currentTimeMillis())
		            .setAutoCancel(true)
		            .setContentTitle("VitalSigns App has started")
		            .setContentText("VitalSigns App has started");
		Notification n = builder.build();

		nm.notify(uniqueId, n);
	}
	

	/* Old stuff. Hopefully the new stuff above will work
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
*/		
	//Remove notification from task bar of the phone

	public void removeNotification(){
		CommonMethods.Log("in removeNotification");	
		NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(uniqueId);
	}
	

	
	//this takes in a String msgstr
 	public void showToast(final String msgstr,final int toastLength) {
		Handler toastHandler = new Handler(context.getMainLooper());
		toastHandler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(context,msgstr,toastLength).show();
			}
		});
	}
	
 	//this takes in a int  msgstr
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
			 editor.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public boolean getFlagShutDown()
			throws Exception {
		SharedPreferences settings=PreferenceManager.getDefaultSharedPreferences(context);
		boolean flagShutDown=settings.getBoolean("flagShutDown", false);
		return flagShutDown;

	}
	// will not work - i'mnot sure how to get this working
	public AlertDialog.Builder showMessage(String mess){
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(context); 
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
	
	public void showAlertDialogOnUiThread(final String mess ){
		((Activity) context).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				showAlertDialog( mess);		
			}
		});
	}
	
	

	public void showAlertDialog(final String mess) {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(context); 
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
