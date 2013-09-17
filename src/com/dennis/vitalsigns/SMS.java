package com.dennis.vitalsigns;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Settings; 
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.MediaPlayer;
import  android.media.ToneGenerator;

import android.app.Activity;
import android.app.ActivityManager.RunningServiceInfo;

import android.content.BroadcastReceiver;
import android.content.res.Resources.NotFoundException;



import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.*;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.HttpEntity;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

public class SMS {

	private Context context;
	
	
	
	public SMS(Context context) {
		this.context=context;
	}

	void sendSMS(String phoneNumber, String message)throws Exception
	{       	
		if(phoneNumber==null){
			return;
		}
		if(phoneNumber.trim().compareTo("")==0){
			return;
		}      
		try{
			String SENT = "SMS_SENT";
			String DELIVERED = "SMS_DELIVERED";
	
			PendingIntent sentPI = PendingIntent.getBroadcast(context, 0,
					new Intent(SENT), 0);
	
			PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0,
					new Intent(DELIVERED), 0);
	
	
			BroadcastReceiver smsSentReceiver= new BroadcastReceiver(){
				@Override
				public void onReceive(Context arg0, Intent arg1) {
					switch (getResultCode())
					{
					case Activity.RESULT_OK:
						Toast.makeText(context, "SMS sent", 
								Toast.LENGTH_SHORT).show();
						break;
					case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
						Toast.makeText(context, "Generic failure", 
								Toast.LENGTH_SHORT).show();
						break;
					case SmsManager.RESULT_ERROR_NO_SERVICE:
						Toast.makeText(context, "No service", 
								Toast.LENGTH_SHORT).show();
						break;
					case SmsManager.RESULT_ERROR_NULL_PDU:
						Toast.makeText(context, "Null PDU", 
								Toast.LENGTH_SHORT).show();
						break;
					case SmsManager.RESULT_ERROR_RADIO_OFF:
						Toast.makeText(context, "Radio off", 
								Toast.LENGTH_SHORT).show();
						break;
					}
				}
			};
	
			BroadcastReceiver smsDeliveredReceiver=new BroadcastReceiver(){
				@Override
				public void onReceive(Context arg0, Intent arg1) {
					switch (getResultCode())
					{
					case Activity.RESULT_OK:
						Toast.makeText(context, "SMS delivered", 
								Toast.LENGTH_SHORT).show();
						break;
					case Activity.RESULT_CANCELED:
						Toast.makeText(context, "SMS not delivered", 
								Toast.LENGTH_SHORT).show();
						break;                        
					}
				}
			};
	
			//---when the SMS has been sent---
			context.registerReceiver(smsSentReceiver, new IntentFilter(SENT));
	
			//---when the SMS has been delivered---
			context.registerReceiver(smsDeliveredReceiver, new IntentFilter(DELIVERED));        
	
			SmsManager sms = SmsManager.getDefault();
			sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);  
			Toast.makeText(context,"SMS sent to " + phoneNumber +" Message:"+ message,Toast.LENGTH_LONG).show();
	
			context.unregisterReceiver(smsSentReceiver);//is there a point in doing this so soon?
			context.unregisterReceiver(smsDeliveredReceiver);//is there a point in doing this so soon?
	
		} catch (Exception e) {
			CommonMethods.Log("Exception in sendSMS "+e.getMessage());
	
		}
	}

	String getMessageForSMS()throws Exception{
	
		long MINIMUM_DISTANCE_CHANGE_FOR_UPDATES = 0; // in Meters
		long MINIMUM_TIME_BETWEEN_UPDATES = 10000; // in Milliseconds
		String message=null;
		try{
			
			TelephonyManager tm= (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
			String phoneNumber = tm.getLine1Number();
			if(phoneNumber==null){
				phoneNumber="";
			}
			message= String.format("Please Call:%1$s",phoneNumber);
			Double lat=0.0;
			Double lon=0.0;
	
	
			LocationManager locationManager=(LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
	
			Criteria criteria = new Criteria ();
			String bestProvider = locationManager.getBestProvider (criteria, true); //true returns a provider that is enabled
			LocationListener mlocListener = new mLocationListener();
			locationManager.requestLocationUpdates( bestProvider, MINIMUM_TIME_BETWEEN_UPDATES, MINIMUM_DISTANCE_CHANGE_FOR_UPDATES, mlocListener,context.getMainLooper());
	
			Location location=null;
			for(int i=0;i<10;i++){// dennis: waiting for a location value. But will timeout after a certain time defined by the loop
				//CommonMethods.Log("In wait loop");
				location = locationManager.getLastKnownLocation(bestProvider); 
				if(location!=null){
					break;
				}
				Thread.sleep(Integer.parseInt(context.getString(R.string.pref_countdown))*1000);
			}
			if(location!=null){
				lat=location.getLatitude();
				lon=location.getLongitude();
	
			}
			locationManager.removeUpdates(mlocListener);// if this is not done battery may drain faster
	
			message= String.format("Emergency. Please Call:"+phoneNumber+". To find the location of the person in google MAPS, search for "+lat+", "+ lon);
		} catch (Exception e) {
			CommonMethods.Log("Exception in sendSMS "+e.getMessage());
	
		}
		if(message.length()>160){
			message=message.substring(0, 160);//if the string is more than 160 characters then an exception will be thrown while sending the SMS. So limit the size
		}
	
		return message;
	}

	private  class mLocationListener implements LocationListener {
	
		@Override
		public void onLocationChanged(Location locFromGps) {
			// called when the listener is notified with a location update from the GPS
		}
	
		@Override
		public void onProviderDisabled(String provider) {
			// called when the GPS provider is turned off (user turning off the GPS on the phone)
		}
	
		@Override
		public void onProviderEnabled(String provider) {
			// called when the GPS provider is turned on (user turning on the GPS on the phone)
		}
	
		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// called when the status of the GPS provider changes
		}
	
	}

	
}
