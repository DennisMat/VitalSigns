package com.dennis.vitalsigns;


import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.hardware.Sensor; //actual device change 1
import android.hardware.SensorEvent; //actual device 
import android.hardware.SensorEventListener; //actual device
/*
import org.openintents.sensorsimulator.hardware.Sensor; // sensor simulator
import org.openintents.sensorsimulator.hardware.SensorEvent;// sensor simulator
import org.openintents.sensorsimulator.hardware.SensorEventListener;// sensor simulator
import org.openintents.sensorsimulator.hardware.SensorManagerSimulator;// sensor simulator
*/
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


public class AccService extends Service
{
	public static boolean isServiceRunning=false; //simplest way to track the service. There might be better ways.
	public static int threshold=0;
	public static int monitorCycles=5;//Sensor is monitored during these cycles
	public static int hibernateCycles=0;//battery saving feature. Does nothing during these cycles. Sensor is un-registered during these cycles. If this is zero  it always works. if it is non-zero it may not work on some phone.
	public static int historySize=500;
	public static int timerPeriod=10;  //seconds
	public static int TimerSleep=10;// milli seconds
	public static String PhoneNumber="777-777-7777";
	public static String[] PhoneNumberArray={"5554","5554","5554"};
	public static boolean[] DialArray={false,false,false};
	public static boolean[] SMSArray={true,true,true};
	public static int CountDown=10; // Count down before dialing.
	public static int TimeBetweenDialing=10;//seconds. this is the time between repeated dialing attempts
	public static boolean messageShowInPopup=true;
	public static boolean remotelog=true;
	public static String logTag="VitApp";
	public static PowerManager.WakeLock mWakeLock = null;

	public TextView textViewThreshold;
	private int CountDownProgress=CountDown;
	private int totalCycles=monitorCycles+hibernateCycles;
	private TimerTask mTimerTask;
	
	private Context context=null;
	
	private int[] statusHistory= new int[historySize];// stores the history in ones and zeros. The ones and zeros are deduced from the accelerometer readings.
	private int[] deltaHistory= new int[historySize];// stores the history of the accelerometer readings.
	
	private Handler toastHandlerOptional;
	private Handler toastHandler;//always shows
	private String Mess="";
	private String messageOptional="";

	private String MonitorStatus="";
	
	private int sumStatus=0;
	private int sumStatusThreshold=275;
	private float xacc=0;
	private float yacc=0;
	private float zacc=0;

	private float xaccPrevious=0;
	private float yaccPrevious=0;
	private float zaccPrevious=0;
	
	private int cycleCount=0;
	
	
	public int status=1;//1=moving 0=not moving
	SensorManager mSensorManager = null;//actual device  //change 2
	//SensorManagerSimulator mSensorManager=null; //sensor simulator
	private SensorEventListener mEventListenerAccelerometer;
	StringBuilder sb=null;
	ToneGenerator tg=null;
	
	
	public static VitalSigns MAIN_ACTIVITY;    
	public static Timer timer=null;
    private boolean IsSensorRegistered=false;    
    private NotificationManager mNM;

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.local_service_started;
    
    // hooks main activity here    
    public static void setMainActivity(VitalSigns activity) 
    {
      MAIN_ACTIVITY = activity;      
    }
    
    /* 
     * not using ipc...but if we use in future
     */
    public IBinder onBind(Intent intent) {
      return null;
    }
    

    @Override 
    public void onCreate() 
    {
      try {
		super.onCreate(); 
		  context = getApplicationContext();
		 mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE );//actual device  //change 3
		 //mSensorManager = SensorManagerSimulator.getSystemService(context, SENSOR_SERVICE);//sensor simulator
		 //mSensorManager.connectSimulator();//sensor simulator        
		 initListener();
		 initToastHandlers();
		 initializeVariables();
		 startService();               
		 showNotification(); // Display a notification about us starting.  We put an icon in the status bar.
		 aquireWakeLock();
	} catch (Exception e) {
	}
    }
    

    
    @Override 
    public void onDestroy() 
    {
      try {
		super.onDestroy();
		  shutdownService();
		  if (MAIN_ACTIVITY != null){
			  //Log.d(getClass().getSimpleName(), "AccService stopped");
		  }
		  // Cancel the persistent notification.
		  mNM.cancel(NOTIFICATION);
		  // Tell the user we stopped.
		  Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
	} catch (Exception e) {
		releaseWakeLock();
	}
    }
    
    /*
     * starting the service
     */
    private void startService()throws Exception{
    	isServiceRunning=true;
    	checkCountDown();
    	 playBeep(500);
    	 mTimerTask= new TimerTask() {
    		public void run() {	    			
    			doWork();     			
    		}
    	};
    	timer=new Timer();
    	timer.schedule(mTimerTask, 0, timerPeriod*1000);
    }
    
    private void doWork(){
    	try{ 

    	//Log( "doWork called at "+timerPeriod+" seconds. Time=" + (new java.util.Date()).getMinutes()+" : " +(new java.util.Date()).getSeconds());
    	if(cycleCount<=monitorCycles || CountDownProgress<CountDown){ //if a countdown is in progress continue monitoring
	    	if(!IsSensorRegistered){
	    		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);//actual device //change 4
	    		//mSensorManager = SensorManagerSimulator.getSystemService(context, SENSOR_SERVICE);//sensor simulator  
	    	 	//mSensorManager.connectSimulator();//sensor simulator
	    		IsSensorRegistered=mSensorManager.registerListener(mEventListenerAccelerometer, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);	
	    		java.util.Date CurrTime= new java.util.Date();
		    	//Log( "Sensor Registered Time= " + CurrTime.getHours()+" : " + CurrTime.getMinutes()+" : " +CurrTime.getSeconds());
			}
    		doMonitor();
    		
        	MonitorStatus="Monitoring Cycle (" +cycleCount+")";
        	if(!(cycleCount<=monitorCycles)){
	        	if(CountDownProgress<CountDown){
	        		MonitorStatus="Monitoring being continued due to start in countdown. Monitoring Cycle(" +cycleCount +")";
	        	}
        	}
        	showInPopup("MonitorStatus: " + MonitorStatus);

    	}else //cycleCount>monitorCycles   	
    	{
    		hibernate();// no monitoring is done here.
    		MonitorStatus="Hibernating Cycle (" +cycleCount+")";
    		showInPopup(" MonitorStatus: " + MonitorStatus);
    	}
    	cycleCount++;
    	if(cycleCount>totalCycles){
    		cycleCount=0;//reset cycleCount 	
    	}
    	
	} catch (Exception e) {
    	Log("Exception in doWork()" + e.getMessage());
      }

    	
    }
    
    private void hibernate(){
    	//Log( "hibernate called at "+timerPeriod+" seconds. Time=" + (new java.util.Date()).getMinutes()+" : " +(new java.util.Date()).getSeconds());
    	stopMonitoring();
    	
    }
    
    private void stopMonitoring(){
    	try{ 
    	if(IsSensorRegistered){ 
    	 mSensorManager.unregisterListener(mEventListenerAccelerometer);
    	 IsSensorRegistered=false;
    	 java.util.Date CurrTime= new java.util.Date();
    	 //Log( "Sensor UnRegistered Time= " + CurrTime.getHours()+" : " + CurrTime.getMinutes()+" : " +CurrTime.getSeconds());
    	}
    	
	} catch (Exception e) {
    	Log("Exception in stopMonitoring()" + e.getMessage());
      }
    }
    

    private void doMonitor() 
    {
        try{        	
        	java.util.Date CurrTime= new java.util.Date();
        	//Log( "doMonitor called at Time=" + CurrTime.getHours()+" : " + CurrTime.getMinutes()+" : " +CurrTime.getSeconds());
        	//Log( "sumStatus="+sumStatus+"status="+status);   	
	    	//if(status==0){
        	if(sumStatus<sumStatusThreshold){	    		
	    		//Log("beep start");
	    		CountDownProgress--;
	    		checkCountDown();
	    		playBeep(50);  		
	    		//Log("beep end");	         
	    	}else{    		
	    		stopBeep();
	    	}
    	
    	} catch (Exception e) {
    	Log("Exception in doMonitor()" +e.getMessage());
      }
    	//
    }
    
    /*
     * shutting down the service
     */
    private void shutdownService() throws Exception
    {
    	releaseWakeLock();
    	stopMonitoring();
    	
    	if (timer != null){
    		timer.cancel();
    	}
    	isServiceRunning=false;
    	playBeep(500);
    	if(isAccServiceRunning()){
	    	stopSelf();
	    	if (VitalSigns.buttonHandler!=null){
	    		VitalSigns.startButtonStatus=true;
	    		VitalSigns.stopButtonStatus=false;    				
	    		VitalSigns.buttonHandler.sendEmptyMessage(0);
	    	}
    	}

      Log( "shutdownService called");
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.local_service_started);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.icon, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, VitalSigns.class), 0);
        
        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.local_service_label),
                       text, contentIntent);

        // Send the notification.
        mNM.notify(NOTIFICATION, notification);
    }

    /** generates a beep using the phones inbuilt tone generator. One can use a sound file but that would increase the size of the app. */
    private void playBeep(int durationMs){
    	try {
    	if (tg == null) {
    		int volume=50; //0 is min and max is 100. this is in percentage.
    		//tg= new ToneGenerator(android.media.AudioManager.STREAM_SYSTEM,volume );
        	tg= new ToneGenerator(android.media.AudioManager.STREAM_SYSTEM,ToneGenerator.MAX_VOLUME );
        }
    	tg.startTone (ToneGenerator.TONE_DTMF_A, durationMs);
    	

        } catch (Exception e) {
        	Log(e.getMessage());
          }
    }
  
    private void checkCountDown()throws Exception{
    	Log("CountDownProgress =" + CountDownProgress);
		if((CountDownProgress>-1) && (CountDownProgress<CountDown)){		    	
	    	showToast("Count down to dial has begun " + CountDownProgress + ".\nMove phone to stop count down");
		}
		/*call only once when the CountDownProgress has reached zero so that 
		 * dailing and SMS is not done infinitely
		 */
		if(CountDownProgress==0){
			
			for(int i=0;i<50;i++){
				playBeep(50);
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		
			
    		if(isAccServiceRunning()){ //stop notifying people if service is stopped
    			boolean isNotified=notifyPeople();
    			if(isNotified){
    				//this statement is done to prevent shutdown form being called after the "Stop" and "Start" button are hit in immediate succession. When that is done what happens is that shutdownService() is called twice.
    				//Once by the stop button and then a few seconds later after notifyPeople() is called. the second time calling is unwarranted since it stop a service that is explicitly started by the user.
    				shutdownService();
    			}
       		}
    	
    	
    	
    	}
    	
    }
    
    private void stopBeep(){
    	try {
			CountDownProgress=CountDown;//reset count down
			if (tg != null) {
				tg.stopTone();
				tg.release();
			    tg = null;
			    //Log("stopTone called in stopBeep");
			}
		} catch (Exception e) {
			Log(e.getMessage());
		}
    }
    
    public boolean isAccServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.dennis.vitalsigns.AccService".equals(service.service.getClassName())) {
            	//AccService.Log(" Service is running in ACC class");
            	return true;            
            }
        }
        //AccService.Log(" Service is not running in ACC class");
        return false;
    }
    
    private void	initToastHandlers()throws Exception{	
		
	      toastHandlerOptional = new Handler(){
				@Override
				public void dispatchMessage(Message msg) {
				    super.dispatchMessage(msg);	
				    Toast toast=null;
				    if(messageShowInPopup){
				    	if(sb==null){
				    	toast = Toast.makeText(context, messageOptional, Toast.LENGTH_LONG);
				    	}else{
				    		toast = Toast.makeText(context, messageOptional +"\n" +sb.toString(), Toast.LENGTH_LONG);
				    	}
					     toast.show();
				    }
				}
			};			
			
			
		      toastHandler = new Handler(){
					@Override
					public void dispatchMessage(Message msg) {
					    super.dispatchMessage(msg);
					    StringBuilder sb = new StringBuilder();
					    int averageDeltaHistory=0;
						for (int i=0; i<deltaHistory.length; i++) { 
							
							if(i+1<deltaHistory.length){
								//sb.append(deltaHistory[i]+",");
								averageDeltaHistory+=deltaHistory[i];
								}
						}
						averageDeltaHistory=averageDeltaHistory/deltaHistory.length;
						sb=sb.append("Average movement=" +averageDeltaHistory);
						Log(Mess);
						Toast toast = Toast.makeText(context, Mess +"\n" +sb.toString(), Toast.LENGTH_LONG);			    	
						toast.show();
					  
					}
				};
			
    	}
    
    private void initializeVariables()throws Exception{
	// initialize arrays with value above the threshold so that count down is not started when app is started	
	 for (int i=0; i<deltaHistory.length; i++) { 
		 deltaHistory[i]=threshold*2;
			statusHistory[i]=1;
		}
	 status=1;
	 logTag=getClass().getSimpleName();
	 
	 if (MAIN_ACTIVITY != null){    
	      mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE); 
	 }
	 
    }
    
	private void initListener() {
		mEventListenerAccelerometer = new SensorEventListener() {
			@Override
			public void onSensorChanged(SensorEvent event) {
				try {
		           // Log("onSensorChanged: ", "onSensorChanged accessed");
		           //Log("x: "+event.values[0] + ", y: " + event.values[1] + ", z: " + event.values[2]);
		            
					if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {//for real device  //change 5
		        	//if (event.type==Sensor.TYPE_ACCELEROMETER) { //for simulator
		            	//Log( "In onSensorChanged" + (new java.util.Date()).getSeconds());		 
						
		            	xaccPrevious=xacc;
		       		 	yaccPrevious=yacc;
		       		 	zaccPrevious=zacc;
		            	xacc=event.values[0];
		            	yacc=event.values[1];
		            	zacc=event.values[2];
		            	//Thread.sleep(TimerSleep);//TimerSleep millisecond delay between checks. Why this works is a mystery. Possibly it spaces out readings sufficiently so that there is no deluge of readings to be crunched.
		            	processValues(xacc,yacc,zacc,xaccPrevious,yaccPrevious,zaccPrevious);
		            	
						/*
						accelVals = lowPass( event.values, accelVals );
						//accelVals = event.values;
		            	xaccPrevious=xacc;
		       		 	yaccPrevious=yacc;
		       		 	zaccPrevious=zacc;
		            	xacc=accelVals[0];
		            	yacc=accelVals[1];
		            	zacc=accelVals[2];
		            	processValues(accelVals,xaccPrevious,yaccPrevious,zaccPrevious);
		            	*/
		            }            
		            
		        } catch (Exception e) {
		        	Log(e.getMessage());
		          } 
			}

			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}
		};
	}
	

    /* Does 3 things:
     * 1)Fill in the last status read
     * 2)shift values of array. Value move from higher indices to low i.e statusHistory[0] has the oldest value
     * 3)Scans the statusHistory values to see if there is a moving status anywhere. Even if one moving status is found the overall value is considered moving 
     * */
	/*
    private void processValuesNew(float[] eventValues,float xp,float yp,float zp)throws Exception{    	
    	     	
    	 float x=eventValues[0];
    	 float y=eventValues[1];
    	 float z=eventValues[2];
    	 
    	int CurrentStatus=0;
    	int delta=(int)((Math.abs(x-xp)+Math.abs(y-yp)+Math.abs(z-zp))*1000);
    	if(delta>threshold){
    		CurrentStatus=1;//Moving 
    	}
 	
    	statusHistory[statusHistory.length-1]=CurrentStatus;
    	deltaHistory[deltaHistory.length-1]=delta;
    	
    	status=0;
    	sumStatus=0;
    	int sumDelta=0;
		for (int i=0; i<statusHistory.length; i++) { 
			// shift value in the array
			if(i+1<statusHistory.length){//prevent index out of bounds
				statusHistory[i]=statusHistory[i+1];
				deltaHistory[i]=deltaHistory[i+1];
				}
			//scan history
			if(statusHistory[i]==1){
				status=1;//moving
				////Log( "Index="+i);
			}
			sumStatus+=statusHistory[i];
			sumDelta+=deltaHistory[i];
		}
    	
		Log( "delta="+delta+" threshold=" +threshold+" sumStatus="+sumStatus+" status="+status+" sumTotalDelta="+sumDelta);
		//Log(""+delta);
    	
    }
    
    
 */
	/*
	 * time smoothing constant for low-pass filter
	 * 0 <= alpha <= 1 ; a smaller value basically means more smoothing
	 * See: http://en.wikipedia.org/wiki/Low-pass_filter#Discrete-time_realization
	 */
	/* 
	static final float ALPHA = 0.20f;
	    protected float[] accelVals;
	
    protected float[] lowPassFilter( float[] input, float[] output ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }
    
    */
    
    /*
    // to filter out acceleration due to gravity
    private voidlowPassfilter(){
        // alpha is calculated as t / (t + dT)
        // with t, the low-pass filter's time-constant
        // and dT, the event delivery rate

        final float alpha = 0.8;

        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];

    }
    */
    
    private void processValues(float x,float y,float z,float xp,float yp,float zp)throws Exception{
    	int CurrentStatus=0;
    	float deltaf=Math.abs(x-xp)+Math.abs(y-yp)+Math.abs(z-zp);
    	int delta= (int)(deltaf*1000);
    
    	if(delta>threshold){
    		CurrentStatus=1;//Moving 
    	}
	
    	//Log("MonitorStatus: " + MonitorStatus + " TotalDelta="+TotalDelta);
 	
    	statusHistory[statusHistory.length-1]=CurrentStatus;
    	deltaHistory[deltaHistory.length-1]=delta;
    	
    	status=0;
    	int localSumStatus=0;
    	int sumDelta=0;
		for (int i=0; i<statusHistory.length; i++) { 
			// shift value in the array
			if(i+1<statusHistory.length){//prevent index out of bounds
				statusHistory[i]=statusHistory[i+1];
				deltaHistory[i]=deltaHistory[i+1];
				}
			//scan history
			if(statusHistory[i]==1){
				status=1;//moving
				////Log( "Index="+i);
			}
			localSumStatus+=statusHistory[i];
			sumDelta+=deltaHistory[i];
		}
		sumStatus=localSumStatus;
		Log( " delta="+delta+" threshold=" +threshold+" sumStatus="+sumStatus+" status="+status+ " sumStatusThreshold=" + sumStatusThreshold + " sumDelta="+sumDelta);
    	
    }
    
    
    /*accelerometer code ends*/
    
    public void instantNotification() {   //If I change the modifier to static, {Context} related problems occur
        //use Toast notification: Need to accept user interaction, and change the duration of show
        Toast toast = Toast.makeText(context, "Count down has begun", Toast.LENGTH_SHORT);
        toast.show();

        
    }
    
    public void showToast(String msgstr){
    	Mess=msgstr;
    	if (toastHandler!=null){
    		toastHandler.sendEmptyMessage(0);
    	}
    }
    
    public void showInPopup(String msgstr ){
    	messageOptional=msgstr;
	    sb = new StringBuilder();	 	    
	    int averageDeltaHistory=0;
		for (int i=0; i<deltaHistory.length; i++) { 
			
			if(i+1<deltaHistory.length){
				//sb.append(deltaHistory[i]+",");
				averageDeltaHistory+=deltaHistory[i];
				}
		}
		averageDeltaHistory=averageDeltaHistory/deltaHistory.length;
		sb=sb.append("Average movement=" +averageDeltaHistory);
	
		
    	//Log(messageOptional +"\n" +sb.toString());
    	if (toastHandlerOptional!=null){
    		toastHandlerOptional.sendEmptyMessage(0);
    	}
    }

    /* TODO: If the battery is charging the app should go into a snooze for 2 hours before it reverts to its normal functioning.
     */
    public boolean isBatteryCharging(Context context) {
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
    }

    public boolean notifyPeople(){ 

    	
    	for(int i=0;i<PhoneNumberArray.length;i++){
    		try {
    		if(DialArray[i]){
    			dialNumber(PhoneNumberArray[i]);	
    		}
    		if(SMSArray[i]){
    			sendSMS(PhoneNumberArray[i],getMessageForSMS());// improve this sentence get person's name and number and GPS	
    		}
    		if(!isAccServiceRunning()){ //stop notifying people if service is stopped
    		 return false;
    		}
			Thread.sleep(TimeBetweenDialing*1000); // seconds between dialing
    		if(!isAccServiceRunning()){
       		 return false;
       		}
			
			} catch (Exception e) { //stop notifying people if service is stopped
				// TODO Auto-generated catch block
				Log("InterruptedException " +e.getMessage());
			}
    	}
    	
    	 return true;
    }
    
    public void dialNumber(String phno){ 
    	if(phno==null){
    		return;
    	}
    	if(phno.trim().compareTo("")==0){
    		return;
    	}  	
        
          try {
			    showToast("Dialing: "+ PhoneNumber);
			    //Log("Dialing: "+ PhoneNumber);      	   
        	  Intent CallIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phno));
        	  CallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	  showToast("About to dial");
          startActivity(CallIntent);
         Log("Dialed number successfully");
          } catch (Exception e) {
        	Log("Exception in DialNumber "+e.getMessage());
            
          }
        
      }
    
 
    //---sends an SMS message to another device---
    private void sendSMS(String phoneNumber, String message)throws Exception
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
 
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
            new Intent(SENT), 0);
 
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
            new Intent(DELIVERED), 0);
 
        
        BroadcastReceiver smsSentReceiver= new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS sent", 
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "Generic failure", 
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service", 
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU", 
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off", 
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
                        Toast.makeText(getBaseContext(), "SMS delivered", 
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getBaseContext(), "SMS not delivered", 
                                Toast.LENGTH_SHORT).show();
                        break;                        
                }
            }
        };
        
        //---when the SMS has been sent---
        registerReceiver(smsSentReceiver, new IntentFilter(SENT));
 
        //---when the SMS has been delivered---
        registerReceiver(smsDeliveredReceiver, new IntentFilter(DELIVERED));        
        
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);  
    	showToast("SMS sent to " + phoneNumber +" Message:"+ message);
    	
    	unregisterReceiver(smsSentReceiver);//is there a point in doing this so soon?
    	unregisterReceiver(smsDeliveredReceiver);//is there a point in doing this so soon?
    	
    } catch (Exception e) {
  	Log("Exception in sendSMS "+e.getMessage());
      
    }
    }

    private String getMessageForSMS()throws Exception{
    	
    	 long MINIMUM_DISTANCE_CHANGE_FOR_UPDATES = 0; // in Meters
    	 long MINIMUM_TIME_BETWEEN_UPDATES = 10000; // in Milliseconds
    	 String message=null;
    	try{
    	Context context = getApplicationContext();
    	TelephonyManager tm= (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
    	String phoneNumber = tm.getLine1Number();
    	if(phoneNumber==null){
    		phoneNumber="";
    	}
    	message= String.format("Please Call:%1$s",phoneNumber);
    	Double lat=0.0;
    	Double lon=0.0;
    	

    	LocationManager locationManager=(LocationManager) getSystemService(Context.LOCATION_SERVICE);

    	Criteria criteria = new Criteria ();
        String bestProvider = locationManager.getBestProvider (criteria, true); //true returns a provider that is enabled
        LocationListener mlocListener = new mLocationListener();
        locationManager.requestLocationUpdates( bestProvider, MINIMUM_TIME_BETWEEN_UPDATES, MINIMUM_DISTANCE_CHANGE_FOR_UPDATES, mlocListener,getMainLooper());
        
        Location location=null;
        for(int i=0;i<10;i++){// waiting for a location value. But will timeout after a certain time defined by the loop
        	//Log("In wait loop");
        	location = locationManager.getLastKnownLocation(bestProvider); 
        	if(location!=null){
        		break;
        	}
        	Thread.sleep(Defaults.GPSWait*1000);
        }
    	if(location!=null){
    		lat=location.getLatitude();
	    	lon=location.getLongitude();
	    	
    	}
    	 locationManager.removeUpdates(mlocListener);// if this is not done battery may drain faster

    	message= String.format("Emergency. Please Call:"+phoneNumber+". To find the location of the person in google MAPS, search for "+lat+", "+ lon);
        } catch (Exception e) {
          	Log("Exception in sendSMS "+e.getMessage());
              
        }
    	if(message.length()>160){
    	message=message.substring(0, 160);//if the string is more than 160 characters then an exception will be thrown while sending the SMS
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

    private void aquireWakeLock()throws Exception{
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, logTag);
            mWakeLock.acquire();
        }

    }
    
    public static void releaseWakeLock(){
    	try {
			mWakeLock.release();
			mWakeLock = null;
		} catch (Exception e) {
		}

    }
    
    static public void Log(String logmessage){
    	try { 
			if(remotelog){
			   	HttpClient httpclient = new DefaultHttpClient();    
			    HttpPost httppost = new HttpPost("http://173.230.190.147/alert/Index.aspx");//remote server to which the variables are logged.
			    try {
			        // Add your data
			        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			        nameValuePairs.add(new BasicNameValuePair("logmessage", logmessage));        
			        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			        // Execute HTTP Post Request
			        //HttpResponse response = httpclient.execute(httppost);
			        httpclient.execute(httppost);
			    } catch (Exception e) {
			    	Log.i(logTag,e.getMessage());
			    } 
				
			}
			else{
				Log.i(logTag,logmessage);
			}
		} catch (Exception e) {
			
		}
    	
    	
    }

}
