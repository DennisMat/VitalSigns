/*adb install C:\Users\dennis\Documents\dennis\dennis\work\Jordan\VitalSigns\sensorsimulator-1.1.1\bin\SensorSimulatorSettings-1.1.1.apk

cd C:\Users\dennis\Documents\dennis\dennis\work\Jordan\android\android-sdk_r12-windows\android-sdk-windows\platform-tools
 * 
 * 
 * 
 * */

package com.dennis.vitalsigns;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;



import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TabHost.TabSpec;
import android.util.Log;
import android.telephony.TelephonyManager;

public class VitalSigns extends Activity{
    /** Called when the activity is first created. */
	

	
	private EditText  editTextThreshold;
	private EditText  editTextHistorySize;
	private EditText  editTextTimerPeriod;
	private EditText  editTextTimerSleep;
	private EditText  editTextMonitorTime;
	private EditText  editTextHibernateTime;
	private EditText  editTextPhoneNumber;
	
	private int arraysize=3;
	private EditText[] editTextPhoneNumberArray=new EditText[arraysize];
	private CheckBox[] checkBoxDialArray=new CheckBox[arraysize];
	private CheckBox[] checkBoxSMSArray=new CheckBox[arraysize];
	
	private EditText  editTextCountdown;
	private EditText  editTextTimeBetweenDialing;
	private CheckBox  checkBoxShowPopup;
	private CheckBox checkBoxRemoteLog;
	
	
	
	private Button buttonSave;
	private Button buttonSaveContact;
	
	private Button buttonResetDefaults;	
	private static Button buttonStart;
	private static Button buttonStop;
	public static boolean startButtonStatus;
	public static boolean stopButtonStatus;
	private TextView textViewErrorMessage;

	public static Handler buttonHandler;
	
	final String tag = "VitalSignsLog";


	private AudioManager maudio;
	private static Intent accServiceIntent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	try {

    	super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
  
        AccService.Log(tag+"  " + "starting activity....");
        
        initializeScreenVariables();
    	
    	//get device id
    	Context context = getApplicationContext();
    	TelephonyManager tm= (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
    	String deviceID= tm.getDeviceId ();

    	//textViewErrorMessage.setText("deviceID = "+deviceID);

    	
    	
    	buttonStart.setEnabled(true);
    	buttonStop.setEnabled(false);
    	
    	//just pass the reference to the service
        AccService.setMainActivity(this);
       //creating an intent for the service
        accServiceIntent = new Intent(this, AccService.class);
    	
        setValues();	

       // for testing only
        /*
    	buttonStart.setEnabled(false);
    	buttonStop.setEnabled(true);             	
        startService(accService);
        */
     
        initializeButtonListners();
        


	} 
	catch(Exception e) {
				
		textViewErrorMessage.setText("Error" + e.getMessage());
	} 
       
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	if(AccService.isServiceRunning){
    		buttonStart.setEnabled(false);
        	buttonStop.setEnabled(true);
    	}
    }
    
    
    private void initializeScreenVariables () throws Exception{

			TabHost tabHost=(TabHost)findViewById(R.id.tabHost);
			tabHost.setup();

			TabSpec spec1=tabHost.newTabSpec("Main");
			spec1.setContent(R.id.tabMain);
			spec1.setIndicator("Main");
			
			 
			TabSpec spec2=tabHost.newTabSpec("Contacts");
			spec2.setContent(R.id.tabContacts);
			spec2.setIndicator("Contacts and simple settings");
			
			TabSpec spec3=tabHost.newTabSpec("Settings");
			spec3.setContent(R.id.tabSettings);
			spec3.setIndicator("Complex Settings"); 
			
			tabHost.addTab(spec1);
			tabHost.addTab(spec2);
			tabHost.addTab(spec3);   
			editTextHistorySize = (EditText) findViewById(R.id.editTextHistorySize);
			editTextThreshold = (EditText) findViewById(R.id.editTextThreshold);
			editTextTimerPeriod = (EditText) findViewById(R.id.editTextTimerPeriod);
			editTextTimerSleep = (EditText) findViewById(R.id.editTextTimerSleep);
			editTextMonitorTime = (EditText) findViewById(R.id.editTextMonitorTime);
			editTextHibernateTime = (EditText) findViewById(R.id.editTextHibernateTime);
			editTextPhoneNumber = (EditText) findViewById(R.id.editTextPhoneNumber1);

			
			int[] editTextPhoneNumberRIdArray={R.id.editTextPhoneNumber1,R.id.editTextPhoneNumber2,R.id.editTextPhoneNumber3};
			int[] checkBoxDialRIdArray={R.id.checkBoxDial1,R.id.checkBoxDial2,R.id.checkBoxDial3};
			int[] checkBoxSMSRIdArray={R.id.checkBoxSMS1,R.id.checkBoxSMS2,R.id.checkBoxSMS3};
			
			for(int i=0;i<arraysize;i++){
				editTextPhoneNumberArray[i]=(EditText)findViewById(editTextPhoneNumberRIdArray[i]);
				checkBoxDialArray[i]=(CheckBox)findViewById(checkBoxDialRIdArray[i]);
				checkBoxSMSArray[i]=(CheckBox)findViewById(checkBoxSMSRIdArray[i]);
			}
			
			editTextTimeBetweenDialing = (EditText) findViewById(R.id.editTextTimeBetweenDialing);
			editTextCountdown = (EditText) findViewById(R.id.editTextCountdown);
			
			checkBoxShowPopup= (CheckBox)findViewById(R.id.checkBoxShowPopup); 
			checkBoxRemoteLog= (CheckBox)findViewById(R.id.checkBoxRemoteLog);
			
			
			textViewErrorMessage=(TextView) findViewById(R.id.textViewErrorMessage);
			buttonSave = (Button) findViewById(R.id.buttonSave);
			buttonSaveContact = (Button) findViewById(R.id.buttonSaveContact);
			
			buttonResetDefaults = (Button) findViewById(R.id.buttonResetDefaults);
			 
			buttonStart= (Button) findViewById(R.id.buttonStart);
			buttonStop= (Button) findViewById(R.id.buttonStop);
    	
    }
    
    private void initializeButtonListners(){

        buttonStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	startApp();
            }
         });
              
        buttonStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	stopService();
            }
          });
        
               
        buttonResetDefaults.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	setDefaultValues();	
            }
          });
        
        
        buttonSaveContact.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	saveButtonClicked();
            }
        });
        
        buttonSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	saveButtonClicked();		
            		
            }
          });
        
        
	      buttonHandler = new Handler(){
				@Override
				public void dispatchMessage(Message msg) {
				    super.dispatchMessage(msg);
				    updateButtonStatus();
				    AccService.Log(tag+"  " + "buttons  reset");
				}
			};
        
    }
    
    
    
    private void startApp(){
    	try {
   		
	    	Calendar calExpiry = Calendar.getInstance();
	    	calExpiry.set(2013, Calendar.APRIL, 10);
	    	Calendar currentcal = Calendar.getInstance();         	
	    	
	        	if(currentcal.before(calExpiry)){
	    			// make sure the service is stopped before it's started again
	    			if(isAccServiceRunning()){
	    				AccService.Log(tag+"  " + "Stopping service");
	    				stopService();
	    			}
	        		
	               startService(accServiceIntent);	                
	               buttonStart.setEnabled(false);
	               buttonStop.setEnabled(true);
	                //remove or hide the app             
	               // finish();
	        	}else{
	        		textViewErrorMessage.setText("This software is past it's expiration date. Please contact the developer");
	        	}
    	
    	} catch(Exception e) {
    		AccService.releaseWakeLock();
			AccService.Log(tag+"  " + e);
			buttonStart.setEnabled(true);
        	buttonStop.setEnabled(false);
		}
    }
    
    private  void stopService(){
    	AccService.Log(tag+"stopService called");
    	try {
    		stopService(accServiceIntent);
    		if(!isAccServiceRunning()){
				buttonStart.setEnabled(true);
				buttonStop.setEnabled(false);
    		}
			//remove or hide the app
			//finish(); 
		} catch (Exception e) {
			AccService.releaseWakeLock();
		}
    }
    
    public static void updateButtonStatus(){
    	if(buttonStart!=null && buttonStop!=null ){
	    	buttonStart.setEnabled(startButtonStatus);
	    	buttonStop.setEnabled(stopButtonStatus);
    	}
    }
    
    private boolean isAccServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.dennis.vitalsigns.AccService".equals(service.service.getClassName())) {
            	AccService.Log(tag+" service is running");
            	return true;            
            }
        }
        AccService.Log(tag+" service is not running");
        return false;
    }

    
    private void saveButtonClicked(){
    	try {
			textViewErrorMessage.setText("");
			AccService.historySize=Integer.parseInt(editTextHistorySize.getText().toString());
			AccService.threshold=Integer.parseInt(editTextThreshold.getText().toString());
			AccService.timerPeriod=Integer.parseInt(editTextTimerPeriod.getText().toString());          			           			
			AccService.TimerSleep=Integer.parseInt(editTextTimerSleep.getText().toString());
			
			AccService.monitorCycles=Integer.parseInt(editTextMonitorTime.getText().toString())*60/AccService.timerPeriod;
			AccService.hibernateCycles=Integer.parseInt(editTextHibernateTime.getText().toString())*60/AccService.timerPeriod;
			
			if(AccService.monitorCycles<1){
				AccService.monitorCycles=60/AccService.timerPeriod;	
			}
			if(AccService.hibernateCycles<0){
				AccService.hibernateCycles=0;
			}
			
			
			AccService.CountDown=Integer.parseInt(editTextCountdown.getText().toString());
			
			if(editTextPhoneNumber.getText().toString().compareTo("911")==0){            				
				textViewErrorMessage.setText("Do not use 911 as the phone number to be dialed");            				           				
			}else
			{
				AccService.PhoneNumber=editTextPhoneNumber.getText().toString();
			}
			
			for(int i=0;i<arraysize;i++){
    			if(editTextPhoneNumberArray[i].getText().toString().compareTo("911")==0){            				
    				textViewErrorMessage.setText("Do not use 911 as the phone number to be dialed");            				           				
    			}else
    			{
    				AccService.PhoneNumberArray[i]=editTextPhoneNumberArray[i].getText().toString();
    				AccService.DialArray[i]=checkBoxDialArray[i].isChecked();
    				AccService.SMSArray[i]=checkBoxSMSArray[i].isChecked();
    			}
			}
			
			AccService.TimeBetweenDialing=Integer.parseInt(editTextTimeBetweenDialing.getText().toString());
			AccService.messageShowInPopup=checkBoxShowPopup.isChecked();
			AccService.remotelog=checkBoxRemoteLog.isChecked();
			
			
			
			//save values to local storage i.e SharedPreferences
			Editor e = getPreferences(MODE_PRIVATE).edit();
			
			
			e.putInt("historySize", AccService.historySize);
			e.putInt("threshold", AccService.threshold);
			e.putInt("timerPeriod", AccService.timerPeriod);
			e.putInt("timerSleep", AccService.TimerSleep);
			e.putInt("monitorTime", AccService.monitorCycles*AccService.timerPeriod/60);
			e.putInt("hibernateTime", AccService.hibernateCycles*AccService.timerPeriod/60);
			e.putInt("CountDown", AccService.CountDown);
			e.putString("PhoneNumber", AccService.PhoneNumber);
			for(int i=0;i<arraysize;i++){
				e.putString("PhoneNumber"+i, AccService.PhoneNumberArray[i]);
				e.putBoolean("Dial"+i, AccService.DialArray[i]);
				e.putBoolean("SMS"+i, AccService.SMSArray[i]);
			}
			e.putInt("TimeBetweenDialing", AccService.TimeBetweenDialing);
			e.putBoolean("messageShowInPopup", AccService.messageShowInPopup);
			e.putBoolean("remotelog", AccService.remotelog);
			
	    	e.commit();
	    	
	    	setFormValues();
			
		} catch(NumberFormatException nfe) {
			
			AccService.Log(tag+" Could not parse " + nfe);
			setDefaultValues();
			textViewErrorMessage.setText("Some values are incorrect.\n Reverting to default values");
		} 
		catch(Exception nfe) {
			AccService.Log(tag+" Exception " + nfe);
			setDefaultValues();
			textViewErrorMessage.setText("Some values are incorrect.\n Reverting to default value");
		} 
    }
    
    private void setValues(){
    	try {
    		
    		SharedPreferences settings = getPreferences(MODE_PRIVATE);
   		
			AccService.historySize=settings.getInt("historySize",Defaults.HistorySize);
			AccService.threshold=settings.getInt("threshold",Defaults.Threshold);;
			AccService.timerPeriod=settings.getInt("timerPeriod",Defaults.TimerPeriod);;  //seconds            			           			
			AccService.TimerSleep=settings.getInt("timerSleep",Defaults.TimerSleep);;
			AccService.monitorCycles=settings.getInt("monitorTime",Defaults.MonitorTime)*60/AccService.timerPeriod;
			AccService.hibernateCycles=settings.getInt("hibernateTime",Defaults.HibernateTime)*60/AccService.timerPeriod;
			
			for(int i=0;i<arraysize;i++){
				AccService.PhoneNumberArray[i]=settings.getString("PhoneNumber"+i,AccService.PhoneNumberArray[i]);
				AccService.DialArray[i]=settings.getBoolean("Dial"+i,AccService.DialArray[i]);
				AccService.SMSArray[i]=settings.getBoolean("SMS"+i,AccService.SMSArray[i]);
			}
			AccService.CountDown=settings.getInt("CountDown",Defaults.CountDown);
			AccService.TimeBetweenDialing=settings.getInt("TimeBetweenDialing",Defaults.TimeBetweenDialing);
			AccService.messageShowInPopup=settings.getBoolean("messageShowInPopup", Defaults.messageShowInPopup);
			AccService.remotelog=settings.getBoolean("remotelog", Defaults.RemoteLog);
					
		setFormValues();
    } catch(Exception nfe) {
		AccService.Log(tag+" Exception " + nfe);
	} 
    }
    
    private void setDefaultValues(){
    	try {
		AccService.historySize=Defaults.HistorySize;
		AccService.threshold=Defaults.Threshold;
		AccService.timerPeriod=Defaults.TimerPeriod;  //seconds            			           			
		AccService.TimerSleep=Defaults.TimerSleep;
		AccService.monitorCycles=Defaults.MonitorTime*60/AccService.timerPeriod;
		AccService.hibernateCycles=Defaults.HibernateTime*60/AccService.timerPeriod;
		AccService.PhoneNumberArray=Defaults.PhoneNumberArray;
		AccService.DialArray=Defaults.DialArray;
		AccService.SMSArray=Defaults.SMSArray;		
		AccService.CountDown=Defaults.CountDown;
		AccService.TimeBetweenDialing=Defaults.TimeBetweenDialing;
		AccService.messageShowInPopup=Defaults.messageShowInPopup;
		AccService.remotelog=Defaults.RemoteLog;		
		
		setFormValues();
	
	    } catch(Exception nfe) {
			AccService.Log(tag+" Exception " + nfe);
		} 
    }

    
	private void setFormValues()throws Exception{
			editTextHistorySize.setText(AccService.historySize+"");
			editTextThreshold.setText(AccService.threshold+"");            			          			
			editTextTimerPeriod.setText(AccService.timerPeriod+"");
			editTextTimerSleep.setText(AccService.TimerSleep+"");
			editTextMonitorTime.setText(AccService.monitorCycles*AccService.timerPeriod/60+"");
			editTextHibernateTime.setText(AccService.hibernateCycles*AccService.timerPeriod/60+"");
			editTextCountdown.setText(AccService.CountDown+"");
			editTextPhoneNumber.setText(AccService.PhoneNumber+"");
			
			for(int i=0;i<arraysize;i++){				
				editTextPhoneNumberArray[i].setText(AccService.PhoneNumberArray[i]+"");
				checkBoxDialArray[i].setChecked(AccService.DialArray[i]);
				checkBoxSMSArray[i].setChecked(AccService.SMSArray[i]);
			}
			editTextTimeBetweenDialing.setText(AccService.TimeBetweenDialing+"");
			checkBoxShowPopup.setChecked(AccService.messageShowInPopup);
			checkBoxRemoteLog.setChecked(AccService.remotelog);
			
		}
    
/*A possible substitute for beep, if the beep is not loud enough the phone should ring. A possible side effect is that the ringing may move the phone sufficient and thus cancelling the alert.  
 * 
 */
    private void playRing(){
		/*
		maudio=(AudioManager)getSystemService(AUDIO_SERVICE);
		maudio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
		//maudio.setRingerMode(AudioManager.RINGER_MODE_SILENT);
		
		Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
	     if(alert == null){
	         // alert is null, using backup
	         alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
	         if(alert == null){  // I can't see this ever being null (as always have a default notification) but just incase
	             // alert backup is null, using 2nd backup
	             alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);               
	         }
	     }

		MediaPlayer  mMediaPlayer = new MediaPlayer();
		 mMediaPlayer.setDataSource(getApplicationContext(), alert);
		 final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		 audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
		 if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
			 mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
			 mMediaPlayer.setLooping(true);
			 mMediaPlayer.prepare();
			 mMediaPlayer.start();
		  }
*/
    }
}