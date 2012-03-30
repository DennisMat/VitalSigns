package com.dennis.vitalsigns;

public class Defaults {
	public static final int HistorySize=500;
	public static final int Threshold=250;
	public static final int  TimerPeriod=10;//seconds
	public static final int  TimerSleep=10;//milli seconds
	public static final int  MonitorTime=1;//minutes
	public static final int  HibernateTime=0;//minutes//zero always works
	public static final int CountDown=10;
	//public static String[] PhoneNumberArray={"5556","5556","5556"};
	public static String[] PhoneNumberArray={"777-777-7777","777-777-7777","777-777-7777"};
	public static boolean[] DialArray={false,false,false};
	public static boolean[] SMSArray={false,false,false};
	public static final int  TimeBetweenDialing=10;//seconds
	public static final boolean  messageShowInPopup=false;
	public static final boolean  RemoteLog=false;
	public static final int GPSWait=10;//seconds Time spent waiting to get a value form the GPS
}
