package com.dennis.vitalsigns;

import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.*;

public class CurrentLocationActivity extends Activity {

	private Button buttonMap;
	private Button buttonCopyLocation;
	private TextView textViewSMSMessage;
	public static final String SMS_STR = "smsStr";
	private CommonMethods mCommonMethods =new CommonMethods(this);
	ProgressDialog progress = null;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_current_location);
		initializeVariables();


		GPSAsyncTask mGPSAsyncTask = new GPSAsyncTask(this);
		mGPSAsyncTask.execute();				

		buttonMap.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				/*
				String geoAddress = "maps.google.com/maps?q=";
				Double[] LatLong=phone.getCurrentLatLong();
				geoAddress += LatLong[0] + "," + LatLong[1];    
				Intent i = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(geoAddress));
				startActivity(i);
				 */
				mCommonMethods.showAlertDialog("This feature is yet to be implemented");
			}
		});

		buttonCopyLocation.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				copyToClipBoard();
			}
		});


	}

	private BroadcastReceiver receiverSMSStr= new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			CommonMethods.Log("receiverSMSStr onReceive() called" );
			if(progress!=null){
				progress.dismiss();
			}
			Bundle extras = intent.getExtras();
			if (extras != null) {
				String smsStr = extras.getString("smsStr");
				textViewSMSMessage.setText(smsStr);			    
			}
		}
	};
	
	@Override
	protected void onResume() {
		super.onResume();
		LocalBroadcastManager.getInstance(this).registerReceiver(receiverSMSStr, new IntentFilter(SMS_STR));
	}

	@Override
	protected void onPause() {
		CommonMethods.Log("onPause called" );
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverSMSStr);
		super.onPause();
	}



	private void initializeVariables(){
		buttonMap = (Button) findViewById(R.id.buttonMap);
		buttonCopyLocation = (Button) findViewById(R.id.buttonCopyLocation);

		textViewSMSMessage=(TextView) findViewById(R.id.textViewSMSMessage);
	}

	private void copyToClipBoard(){
		Location location=mCommonMethods.getStoredLocation();
		String geoAddress = location.getLatitude() + "," + location.getLongitude(); 
		android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CurrentLocationActivity.CLIPBOARD_SERVICE); 
		android.content.ClipData clip = android.content.ClipData.newPlainText("latLong",geoAddress);
		clipboard.setPrimaryClip(clip);
		CommonMethods mCommonMethods =new CommonMethods(this);
		mCommonMethods.showAlertDialog(getString(R.string.location_copy_paste));	
	}


	private class GPSAsyncTask extends AsyncTask<Void, Void, Void> {
		private Context mContext;
		public GPSAsyncTask(Context context) {
			mContext = context;
		} 

		@Override
		protected void onPreExecute(){
			progress = new ProgressDialog(mContext);		
			progress.setMessage((CurrentLocationActivity.this).getString(R.string.fetch_gps));
			progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progress.setIndeterminate(true);
			progress.show();	
		}

		@Override
		protected Void doInBackground(Void... args) {
			final Phone phone = new Phone(mContext);
			String smsStr=phone.getMessageForSMS();// this takes time.			

			Intent intent = new Intent(SMS_STR);
			intent.putExtra("smsStr",smsStr);
			LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
			return null;		
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			//do stuff here
			super.onProgressUpdate(values);// this line is always the last
		}

		@Override
		protected void onPostExecute(Void result) {
			if(progress!=null){
				progress.dismiss();
			}

		}

	}

}
