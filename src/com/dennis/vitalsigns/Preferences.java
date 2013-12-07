package com.dennis.vitalsigns;



import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;

public class Preferences 
    extends PreferenceActivity 
    implements OnSharedPreferenceChangeListener {
	
	private CommonMethods mCommonMethods = null;
	//most variable are static because when a setting is changed we want it to take effect right away.
	public static int arraySize=3;
	public static String[] phoneNumberArray=new String[arraySize];
	public static boolean[] dialArray= new boolean[arraySize];
	public static boolean[] SMSArray= new boolean[arraySize];
	
	/** in seconds.	 * 
	 */
	public static int timeBetweenDialing = 0;
	public static boolean messageShowInPopup = false;
	public static String logTag = "VitalSignsTag";
	public static boolean  remoteLog;

	/** The number of beeps before which the dialing and sms'ing starts.
	 */
	public static int countDown = 0; // Count down before dialing.
	/** in seconds.
	 *  time between monitoring session when in continous monitoring mode.
	 */
	public static int timeBetweenMonitoringSessions = 0;
	/** in minutes.
	 *  time during which the phone does not monitor for signal. Used for saving batter life. 
	 *   Set zero for continous monitoring, but this will drain the smartphone battery faster.
	 */
	public static int hibernateTime=0;
	public Context context = null;

	
	
	public Preferences(Context context) {
        this.context = context;
        mCommonMethods = new CommonMethods(context);
    } 
	
/**
 * Dennis: and empty constructor is needed, without which an exception is thrown if the pref activity is opened from the app
 */
	public Preferences() {
		context =this;
		mCommonMethods = new CommonMethods(context);
    } 
	

   @SuppressWarnings("deprecation")
@Override
   protected void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);

    this.addPreferencesFromResource(R.xml.preferences);
    this.initSummaries(this.getPreferenceScreen());

    this.getPreferenceScreen().getSharedPreferences()
      .registerOnSharedPreferenceChangeListener(this);
    }

  /**
    * Set the summaries of all preferences
    */
  private void initSummaries(PreferenceGroup pg) {
    for (int i = 0; i < pg.getPreferenceCount(); ++i) {
    Preference p = pg.getPreference(i);
    if (p instanceof PreferenceGroup){
      this.initSummaries((PreferenceGroup) p); // recursion
    }else{
      this.setSummary(p);
    }
    }
  }

  /**
    * Set the summaries of the given preference
    */
  private void setSummary(Preference pref) {
    // react on type or key
	  /*
      if (pref instanceof ListPreference) {
      ListPreference listPref = (ListPreference) pref;
      pref.setSummary(listPref.getEntry());
      }
      */
      
      if (pref instanceof EditTextPreference) {
          EditTextPreference editTextPref = (EditTextPreference) pref;
          
          String strShowValue="Current Value=";
          
          String summary=pref.getSummary().toString();
          if(summary.indexOf("("+strShowValue)!=-1){
        	  summary=summary.substring(0,summary.indexOf("("+strShowValue))+ "("+strShowValue + editTextPref.getText() + ")";
          }else{
        	  summary=summary+ "("+strShowValue + editTextPref.getText() + ")";
          }
          
          pref.setSummary(summary); 
      }
      
  }

  /**
    * used to change the summary of a preference
    */
  @SuppressWarnings("deprecation")
  public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
    Preference pref = findPreference(key);
    this.setSummary(pref);
    //if the hibernatetime has changed then we need to reschedule the alarm manager
    mCommonMethods.scheduleRepeatingMonitoringSessions();
    loadValuesFromStorage();// we want all the values to take effect right away.
  }
  
  
	public void loadValuesFromStorage() {
		SharedPreferences settings=PreferenceManager.getDefaultSharedPreferences(context);
		for(int i=0;i<arraySize;i++){

			phoneNumberArray[i]=settings.getString("key_ph"+(i+1),context.getString(R.string.pref_ph));
			dialArray[i]=settings.getBoolean("key_dial"+(i+1),Boolean.parseBoolean(context.getString(R.string.pref_dial)));
			SMSArray[i]=settings.getBoolean("key_sms"+(i+1),Boolean.parseBoolean(context.getString(R.string.pref_sms)));
		}


		timeBetweenDialing=Integer.parseInt(settings.getString("key_timebetweendialing",context.getString(R.string.pref_timebetweendialing)));
		hibernateTime=Integer.parseInt(settings.getString("key_hibernatetime",context.getString(R.string.pref_hibernatetime)));
		countDown=Integer.parseInt(settings.getString("key_countdown",context.getString(R.string.pref_countdown)));
		timeBetweenMonitoringSessions=Integer.parseInt(settings.getString("key_timebetweenmonitoring",context.getString(R.string.pref_timebetweenmonitoring)));

		messageShowInPopup=settings.getBoolean("key_showpopup", Boolean.parseBoolean(context.getString(R.string.pref_showpopup)));
		remoteLog=settings.getBoolean("key_remotelog", Boolean.parseBoolean(context.getString(R.string.pref_remotelog)));

	}

}