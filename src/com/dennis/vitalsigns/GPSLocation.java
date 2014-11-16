package com.dennis.vitalsigns;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class GPSLocation {

	private Context context;
	private CommonMethods mCommonMethods=null;
	private List<Location> locations = null;
	private LocationManager locationManager = null;


	public GPSLocation(Context context) {
		this.context=context;
		mCommonMethods =new CommonMethods(context);
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	}

	Location getLocation(){
		Location location=getLocation(false);//takes time

		if(location==null){
			location=getLocation(true);
		}

		if(location!=null){		
			mCommonMethods.updateLocation(location);
		}
		
		if(location==null){
			location=mCommonMethods.getStoredLocation();
			CommonMethods.Log("stored  lat long has the following values: lat="+location.getLatitude() +" lon="+location.getLongitude() );
		}
		return location;
	}



	public Location getLocation(boolean immediate) {
		CommonMethods.Log("in getLocation, immediate " + immediate);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.NO_REQUIREMENT);
		List<String> providers = locationManager.getProviders(criteria, true);
		locations = new ArrayList<Location>();
		List<LocationListener> locListeners= new ArrayList<LocationListener>(); 
	
		CommonMethods.Log(" providers.size()="+providers.size());
		for (String provider : providers) {
			Location location=null;
			if(immediate){
				location = locationManager.getLastKnownLocation(provider);//this is generally quick, but not always.
			}else{

				LocationListener mLocationListener = new LocationListenerImp();
				// this takes time, the result is obtained in onLocationChanged(Location location) 
				locationManager.requestSingleUpdate(provider, mLocationListener, context.getMainLooper());
				locListeners.add(mLocationListener);
				CommonMethods.Log("after requestSingleUpdate. provider = " + provider );
			}

			if (location != null && location.getAccuracy()!=0.0) {
				locations.add(location);
			}
		}
		
		
		//LocationListener mLocationListener = new LocationListenerImp();
		//locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, mLocationListener, context.getMainLooper());

		// we wait for a while for  onLocationChanged to be called multiple time for each provider.
		if(providers.size()>0){			
			for(int i=0;i<Preferences.gpsWaitTime;i+=5){//wait in increments of 5 seconds		
				try {
					CommonMethods.Log("in getLocation, Thread loop = " +i +" seconds locations.size()="+locations.size());
					if(locations.size()==providers.size()){
						break;
					}
					Thread.sleep(5*1000);
					
				} catch (Exception e) {
				}
			}
			
			// after waiting for the gps, remove any listener that may still be listening.
			for (LocationListener locListener : locListeners) {
				CommonMethods.Log("making sure that all listeners are removed.");
				locationManager.removeUpdates(locListener);
			}
		}

		Collections.sort(locations, new Comparator<Location>() {
			@Override
			public int compare(Location location, Location location2) {
				return (int) (location.getAccuracy() - location2.getAccuracy());//sort ascending
			}
		});
		if (locations.size() > 0) {
			for (int i=0;i<locations.size();i++) {
				CommonMethods.Log("provider = " +locations.get(i).getProvider() + " accuracy = " + locations.get(i).getAccuracy());
				
			}
			/* Dennis: The least accuracy figure is at the top.
			As I understand, the smaller the accuracy figure, higher is the correctness of the location.
			It is possible for the GPS to have a higher figure (i.e less correct location) that the network
			 */
			return locations.get(0);
		}
		return null;
	}	


	private  class LocationListenerImp implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			// called when the listener is notified with a location update from the GPS
			//this is where one gets the result of requestSingleUpdate()
			CommonMethods.Log("in onLocationChanged, provider = " + location.getProvider() );
			locationManager.removeUpdates(this);
			locations.add(location);
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
