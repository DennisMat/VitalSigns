package com.dennis.vitalsigns;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.*;

public class CurrentLocationActivity extends Activity {

	private Button buttonMap;
	private Button buttonCopyLocation;
	private TextView textViewSMSMessage;
	private CommonMethods mCommonMethods =new CommonMethods(this);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_current_location);
		initializeVariables();
		final Phone phone = new Phone(this);
		textViewSMSMessage.setText(phone.getMessageForSMS());
		
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
	
	
	private void initializeVariables(){
			buttonMap = (Button) findViewById(R.id.buttonMap);
			buttonCopyLocation = (Button) findViewById(R.id.buttonCopyLocation);
			
			textViewSMSMessage=(TextView) findViewById(R.id.textViewSMSMessage);
	}
	
	private void copyToClipBoard()
	{
		Phone phone = new Phone(this);
	
	Double[] latLong=phone.getCurrentLatLong();
	String geoAddress = latLong[0] + "," + latLong[1]; 
	
	android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CurrentLocationActivity.CLIPBOARD_SERVICE); 
    android.content.ClipData clip = android.content.ClipData.newPlainText("latLong",geoAddress);
    clipboard.setPrimaryClip(clip);
    CommonMethods mCommonMethods =new CommonMethods(this);
    mCommonMethods.showAlertDialog(getString(R.string.location_copy_paste));	
    
    
	}



}
