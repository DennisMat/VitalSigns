package com.dennis.vitalsigns;



import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.PreferenceManager;
import android.net.Uri;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;
import android.location.Location;
import android.app.Activity;
import android.content.BroadcastReceiver;



public class Phone {

	private Context context;
	private CommonMethods mCommonMethods=null;


	public Phone(Context context) {
		this.context=context;
		mCommonMethods=new CommonMethods(context);
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
						mCommonMethods.showToast( "SMS sent", 
								Toast.LENGTH_SHORT);
						CommonMethods.Log( "SMS sent") ;
						break;
					case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
						mCommonMethods.showToast( "Generic failure", 
								Toast.LENGTH_SHORT);
						CommonMethods.Log( "Generic failure") ;
						break;
					case SmsManager.RESULT_ERROR_NO_SERVICE:
						mCommonMethods.showToast( "No service", 
								Toast.LENGTH_SHORT);
						CommonMethods.Log( "No service") ;
						break;
					case SmsManager.RESULT_ERROR_NULL_PDU:
						mCommonMethods.showToast( "Null PDU", 
								Toast.LENGTH_SHORT);
						CommonMethods.Log( "Null PDU") ;
						break;
					case SmsManager.RESULT_ERROR_RADIO_OFF:
						mCommonMethods.showToast( "Radio off", 
								Toast.LENGTH_SHORT);
						CommonMethods.Log( "Radio off") ;
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
						mCommonMethods.showToast( "SMS delivered", 
								Toast.LENGTH_SHORT);
						break;
					case Activity.RESULT_CANCELED:
						mCommonMethods.showToast( "SMS not delivered", 
								Toast.LENGTH_SHORT);
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
			mCommonMethods.showToast("SMS sent to " + phoneNumber +" Message:"+ message,Toast.LENGTH_LONG);

			context.unregisterReceiver(smsSentReceiver);//is there a point in doing this so soon?
			context.unregisterReceiver(smsDeliveredReceiver);//is there a point in doing this so soon?

		} catch (Exception e) {
			CommonMethods.Log("Exception in sendSMS "+e.getMessage());

		}
	}


	String getMessageForSMS(){
		String message=null;
		try{

			TelephonyManager tm= (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
			String phoneNumber = tm.getLine1Number();
			if(phoneNumber==null){
				phoneNumber="";
			}
			message= String.format("Please Call:%1$s",phoneNumber);
			GPSLocation mGPSLocation = new GPSLocation(context);

			Location location=mGPSLocation.getLocation();

			message= String.format("Emergency. Please Call:"+phoneNumber+". To find the location of the person in google MAPS, search for "+location.getLatitude()+", "+ location.getLongitude());
		} catch (Exception e) {
			CommonMethods.Log("Exception in getMessageForSMS() "+e.getMessage());

		}
		if(message.length()>160){
			message=message.substring(0, 160);//if the string is more than 160 characters then an exception will be thrown while sending the SMS. So limit the size
		}

		return message;
	}


	String getMessageAdditionalForSMS(int i){
		String messageAdditional=PreferenceManager.getDefaultSharedPreferences(context).getString("key_sms_additional" + i,"");
		return messageAdditional;
	}

	public void dialNumber(String phoneNumber) {
		if (phoneNumber == null) {
			return;
		}
		if (phoneNumber.trim().compareTo("") == 0) {
			return;
		}

		try {
			CommonMethods.Log("Dialing: "+ phoneNumber);
			Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"
					+ phoneNumber));
			callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			mCommonMethods.showToast("About to dial" +phoneNumber,Toast.LENGTH_SHORT);
			context.startActivity(callIntent);
			CommonMethods.Log("Dialed "+phoneNumber+" successfully");
		} catch (Exception e) {
			CommonMethods.Log("Exception in DialNumber " + e.getMessage());

		}

	}


	String getMessageForSMS(Location location){

		String message=null;
		try{

			TelephonyManager tm= (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
			String phoneNumber = tm.getLine1Number();
			if(phoneNumber==null){
				phoneNumber="";
			}
			message= String.format("Please Call:%1$s",phoneNumber);

			message= String.format("Emergency. Please Call:"+phoneNumber+". To find the location of the person in google MAPS, search for "+location.getLatitude()+", "+ location.getLongitude());
		} catch (Exception e) {
			CommonMethods.Log("Exception in getMessageForSMS(Location location) "+e.getMessage());

		}
		if(message.length()>160){
			message=message.substring(0, 160);//if the string is more than 160 characters then an exception will be thrown while sending the SMS. So limit the size
		}

		return message;

	}

}
