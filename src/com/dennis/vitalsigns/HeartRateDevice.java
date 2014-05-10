package com.dennis.vitalsigns;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.support.v4.content.LocalBroadcastManager;

public class HeartRateDevice {


	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private String mBluetoothDeviceAddress;
	private BluetoothGatt mBluetoothGatt;
	private int mConnectionState = STATE_DISCONNECTED;

	private static final int STATE_DISCONNECTED = 0;
	private static final int STATE_CONNECTING = 1;
	private static final int STATE_CONNECTED = 2;
	private Context mContext;
	private String deviceAddress=null;
	private int heartRate=0;// always initilize to zero - this is important. Should this be volatie?

	public final static String ACTION_GATT_CONNECTED =
			"com.example.bluetooth.le.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_DISCONNECTED =
			"com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED =
			"com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_AVAILABLE =
			"com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
	public final static String EXTRA_DATA =
			"com.example.bluetooth.le.EXTRA_DATA";

	public final static UUID UUID_HEART_RATE_MEASUREMENT =
			UUID.fromString(GattAttributes.HEART_RATE_MEASUREMENT);
	private CommonMethods mCommonMethods=null;


	
	public HeartRateDevice(Context mContext, String deviceAddress) {
		this.mContext=mContext;		
		this.deviceAddress=deviceAddress;
		mCommonMethods= new CommonMethods(mContext);
	}
	
	
	

	public boolean getPersonHeartRateEmergencyStatus() {
	
		int heartRate= getHeartRate();//this may take time.
		CommonMethods.Log("heartRateLow=" + Preferences.heartRateLow + " heartRate=" + heartRate + " heartRateHigh=" + Preferences.heartRateHigh);
		Intent intent = new Intent(VitalSignsActivity.HEART_RATE_DISPLAY_UPDATE);
		intent.putExtra("heartRate",heartRate);
		LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
		if((heartRate<=Preferences.heartRateLow) || (heartRate>=Preferences.heartRateHigh)){
			return true;
		}else{
			return false;
		}

	} 
	
	
	/**This method will return a figure and is to be used for testing only. Set up a file with a heart rate and put it on a website where it can be read.
	 * 
	 * @return
	 */
	private int getDummyHeartRate(){
		CommonMethods.Log("Obtaining heart rate ...");
		try {
			Thread.sleep(10000);// this will simulate a lag for obtaining a reading from a heart rate monitor.
			URL url = new URL("http://photonshift.com/heart.txt");

			BufferedReader in = new BufferedReader(
			new InputStreamReader(url.openStream()));

			String inputLine;
			while ((inputLine = in.readLine()) != null){
				CommonMethods.Log("heart rate read from file is " + inputLine);
				heartRate=Integer.parseInt(inputLine);
			}
			in.close();
			
		} catch (Exception e) {

		}
		
		return heartRate;
	}

	public int getHeartRate(){
		CommonMethods.Log( "in getHeartRate()");
		if(initialize()){
			if(connect(deviceAddress)){
				for(int i=0;i<Preferences.heartRateWaitTime;i++){//This is more than adequate time to read many heart rate values
					CommonMethods.Log( "loop " + i+ " heartRate="+heartRate);
					if(heartRate>0){//as soon  as any value is available break out of loop.
						break;
					}
					try {
						Thread.sleep(1000); 
					} catch (InterruptedException e) {
					}
				}
				
				CommonMethods.Log( "just before closing connection");
				disconnect() ;
				close();
				
			}
		}

		return heartRate;
	}

	private void loopThroughServicesAndCharacteristics() {
		List<BluetoothGattService> gattServices =mBluetoothGatt.getServices();
		CommonMethods.Log( "Number of services = " + gattServices.size());
		for (BluetoothGattService gattService : gattServices) {
			List<BluetoothGattCharacteristic> gattCharacteristics =
					gattService.getCharacteristics();
			CommonMethods.Log( "Number of characteristics = " + gattCharacteristics.size());
			// Loops through available Characteristics.
			for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {   	                    	                
				final int charaProp = gattCharacteristic.getProperties();
				if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
					//CommonMethods.Log( "Uuid = " + gattCharacteristic.getUuid());
					if (mBluetoothAdapter != null || mBluetoothGatt != null) {

						//read the value at BluetoothGattCallback#onCharacteristicRead    	                	 
						if (UUID_HEART_RATE_MEASUREMENT.equals(gattCharacteristic.getUuid())) {

							/* Dennis: setting the descriptor before doing the read readCharacteristic is a required step.
							 * If one doesn't do one cannot read the characteristic.
							 * Don't know why at this point.
							 */
							mBluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);
							BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(
									UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
							descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
							mBluetoothGatt.writeDescriptor(descriptor);

							CommonMethods.Log( "about to call heart rate readCharacteristic");
							mBluetoothGatt.readCharacteristic(gattCharacteristic);
						}
					}
				}

			}
		}
	}

	public String extractHeartRate(BluetoothGattCharacteristic characteristic){

		if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
			int flag = characteristic.getProperties();
			int format = -1;
			if ((flag & 0x01) != 0) {
				format = BluetoothGattCharacteristic.FORMAT_UINT16;
				CommonMethods.Log( "Heart rate format UINT16.");
			} else {
				format = BluetoothGattCharacteristic.FORMAT_UINT8;
				CommonMethods.Log( "Heart rate format UINT8.");
			}			
			heartRate= characteristic.getIntValue(format, 1);
			CommonMethods.Log( String.format("Received heart rate: %d", heartRate));
		}


		return "";
	}



	public boolean initialize() {
		CommonMethods.Log( "in initialize()");
		if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			CommonMethods.Log( "Error bluetooth not supported");
			return false;
		}
		// Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
		// BluetoothAdapter through BluetoothManager.
		final BluetoothManager bluetoothManager =
				(BluetoothManager)mContext.getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		// Checks if Bluetooth is supported on the device.
		if (mBluetoothAdapter == null) {
			CommonMethods.Log( "Error bluetooth not supported");
			return false;
		}


		return true;
	}

	public boolean connect(final String address) {
		CommonMethods.Log( "in connect()");
		if (mBluetoothAdapter == null || address == null) {
			CommonMethods.Log( "BluetoothAdapter not initialized or unspecified address.");
			return false;
		}

		// Previously connected device.  Try to reconnect.
		if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
				&& mBluetoothGatt != null) {
			CommonMethods.Log( "Trying to use an existing mBluetoothGatt for connection.");
			if (mBluetoothGatt.connect()) {
				mConnectionState = STATE_CONNECTING;
				return true;
			} else {
				return false;
			}
		}

		final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		if (device == null) {
			CommonMethods.Log( "Device not found.  Unable to connect.");
			return false;
		}
		// We want to directly connect to the device, so we are setting the autoConnect
		// parameter to false.
		mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
		CommonMethods.Log( "Trying to create a new connection.");
		mBluetoothDeviceAddress = address;
		mConnectionState = STATE_CONNECTING;
		return true;
	}

	/**
	 * Disconnects an existing connection or cancel a pending connection. The disconnection result
	 * is reported asynchronously through the
	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 * callback.
	 */
	public void disconnect() {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			CommonMethods.Log( "in disconnect(). BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.disconnect();
	}

	/**
	 * After using a given BLE device, the app must call this method to ensure resources are
	 * released properly.
	 */
	public void close() {
		if (mBluetoothGatt == null) {
			return;
		}
		mBluetoothGatt.close();
		mBluetoothGatt = null;
	}

	/**
	 * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
	 * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
	 * callback.
	 *
	 * @param characteristic The characteristic to read from.
	 */
	public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			CommonMethods.Log( "in readCharacteristic(). BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.readCharacteristic(characteristic);
	}



	// Implements callback methods for GATT events that the app cares about.  For example,
	// connection change and services discovered.
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			CommonMethods.Log( "in onConnectionStateChange()");
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				//broadcastUpdate(intentAction);
				if (mBluetoothAdapter != null && mBluetoothGatt != null) {
				CommonMethods.Log( "Connected to GATT server.");
				// Attempts to discover services after successful connection.
				CommonMethods.Log( "Attempting to start service discovery:");
				mBluetoothGatt.discoverServices();
				}

			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				CommonMethods.Log( "Disconnected from GATT server.");
				// broadcastUpdate(intentAction);
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				// broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
				CommonMethods.Log( "in onServicesDiscovered");
				loopThroughServicesAndCharacteristics();
			} else {
				CommonMethods.Log( "onServicesDiscovered received: " + status);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic,
				int status) {
			CommonMethods.Log( "in onCharacteristicRead");
			if (status == BluetoothGatt.GATT_SUCCESS) {
				CommonMethods.Log( "Characteristic sucessfully read");
				extractHeartRate(characteristic);
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			CommonMethods.Log( "onCharacteristicChanged");
			extractHeartRate(characteristic);
		}
	};



}
