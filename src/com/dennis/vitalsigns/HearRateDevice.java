package com.dennis.vitalsigns;

import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

public class HearRateDevice {


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
			UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

	private final static String TAG = "heart rate class";




	public HearRateDevice(Context mContext, String deviceAddress) {
		this.mContext=mContext;
		this.deviceAddress=deviceAddress;
	}

	public void getheartRate(){
		if(initialize()){
			if(connect(deviceAddress)){


				try {
					Thread.sleep(20000);
				} catch (InterruptedException e) {
				}

				Log.i(TAG, "just before closing connection");
				disconnect() ;
				close();
			}
		}
	}

	private void listServicesAndCharacteristics() {
		List<BluetoothGattService> gattServices =mBluetoothGatt.getServices();
		Log.i(TAG, "Number of services = " + gattServices.size());
		for (BluetoothGattService gattService : gattServices) {
			List<BluetoothGattCharacteristic> gattCharacteristics =
					gattService.getCharacteristics();
			Log.i(TAG, "Number of characteristics = " + gattCharacteristics.size());
			// Loops through available Characteristics.
			for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {   	                    	                
				final int charaProp = gattCharacteristic.getProperties();
				if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
					//Log.i(TAG, "Uuid = " + gattCharacteristic.getUuid());
					if (mBluetoothAdapter != null || mBluetoothGatt != null) {

						//read the value at BluetoothGattCallback#onCharacteristicRead    	                	 
						if (UUID_HEART_RATE_MEASUREMENT.equals(gattCharacteristic.getUuid())) {


							/* Dennis: setting the descriptor before doing the read readCharacteristic is a required step.
							 * If one doesn't do one cannot read the characteristic.
							 * Don't know why at this point.
							 */
							mBluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);
							BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(
									UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
							descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
							mBluetoothGatt.writeDescriptor(descriptor);

							Log.i(TAG, "about to call heart rate readCharacteristic");
							mBluetoothGatt.readCharacteristic(gattCharacteristic);
						}
					}
				}

			}
		}
	}

	public String printHeartRate(BluetoothGattCharacteristic characteristic){


		if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
			int flag = characteristic.getProperties();
			int format = -1;
			if ((flag & 0x01) != 0) {
				format = BluetoothGattCharacteristic.FORMAT_UINT16;
				Log.i(TAG, "Heart rate format UINT16.");
			} else {
				format = BluetoothGattCharacteristic.FORMAT_UINT8;
				Log.i(TAG, "Heart rate format UINT8.");
			}
			final int heartRate = characteristic.getIntValue(format, 1);
			Log.i(TAG, String.format("Received heart rate: %d", heartRate));
		}


		return "";
	}



	public boolean initialize() {


		if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Log.w(TAG, "Error bluetooth not supported");
			return false;
		}

		// Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
		// BluetoothAdapter through BluetoothManager.
		final BluetoothManager bluetoothManager =
				(BluetoothManager)mContext.getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		// Checks if Bluetooth is supported on the device.
		if (mBluetoothAdapter == null) {
			Log.w(TAG, "Error bluetooth not supported");
			return false;
		}


		return true;
	}

	public boolean connect(final String address) {
		if (mBluetoothAdapter == null || address == null) {
			Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
			return false;
		}

		// Previously connected device.  Try to reconnect.
		if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
				&& mBluetoothGatt != null) {
			Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
			if (mBluetoothGatt.connect()) {
				mConnectionState = STATE_CONNECTING;
				return true;
			} else {
				return false;
			}
		}

		final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		if (device == null) {
			Log.w(TAG, "Device not found.  Unable to connect.");
			return false;
		}
		// We want to directly connect to the device, so we are setting the autoConnect
		// parameter to false.
		mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
		Log.d(TAG, "Trying to create a new connection.");
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
			Log.w(TAG, "BluetoothAdapter not initialized");
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
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.readCharacteristic(characteristic);
	}









	// Implements callback methods for GATT events that the app cares about.  For example,
	// connection change and services discovered.
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

			if (newState == BluetoothProfile.STATE_CONNECTED) {
				//broadcastUpdate(intentAction);
				Log.i(TAG, "Connected to GATT server.");
				// Attempts to discover services after successful connection.
				Log.i(TAG, "Attempting to start service discovery:" +
						mBluetoothGatt.discoverServices());

			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				Log.i(TAG, "Disconnected from GATT server.");
				// broadcastUpdate(intentAction);
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				// broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
				Log.i(TAG, "in onServicesDiscovered");
				listServicesAndCharacteristics();
			} else {
				Log.w(TAG, "onServicesDiscovered received: " + status);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic,
				int status) {
			Log.w(TAG, "in onCharacteristicRead");
			if (status == BluetoothGatt.GATT_SUCCESS) {
				//broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
				Log.w(TAG, "Characteristic sucessfully read");
				printHeartRate(characteristic);
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
				BluetoothGattCharacteristic characteristic) {
			// broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
			Log.w(TAG, "onCharacteristicChanged");
			printHeartRate(characteristic);
		}
	};













}
