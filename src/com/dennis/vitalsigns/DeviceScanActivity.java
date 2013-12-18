package com.dennis.vitalsigns;



import java.util.ArrayList;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

public class DeviceScanActivity extends Activity {

	private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private Button buttonScan;
    private TextView textViewDeviceSelected;
    private TextView textViewDeviceListMessage;
    
    private ProgressDialog progress;
    
    
    private static final int REQUEST_ENABLE_BT = 1;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.device_scan);
	
        mHandler = new Handler();
        buttonScan = (Button) findViewById(R.id.buttonScan);
        textViewDeviceSelected=(TextView) findViewById(R.id.textViewDeviceSelected);
        textViewDeviceListMessage=(TextView) findViewById(R.id.textViewDeviceListMessage);
        progress = new ProgressDialog(this);
        
        buttonScan.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				startScan();
				
			}
		});
        
        
	}
	
	private void startScan(){
		

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Error bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Error bluetooth not supported", Toast.LENGTH_SHORT).show();
            
            return;
        }else{
        	Toast.makeText(this, "BlueTooth supported!", Toast.LENGTH_SHORT).show();
        }
        
        
        progress.setMessage("Scanning for a heart rate device. Please wait");
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.show();
        
        // Stops scanning after a pre-defined scan period.
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);               
                progress.dismiss();
                
                if(mLeDeviceListAdapter.getCount()>0){
                textViewDeviceListMessage.setText("Please select a device (by touching the device name) from the list below.");
                }else{
                	textViewDeviceListMessage.setText("No devices found. You may try again."); 
                }
                
                
                
            }
        }, Preferences.deviceScanTime*1000);


        
        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        
        final ListView listviewHearRateMonitors = (ListView) findViewById(R.id.listviewHearRateMonitors);


        listviewHearRateMonitors.setAdapter(mLeDeviceListAdapter);

        listviewHearRateMonitors.setOnItemClickListener(
        		new AdapterView.OnItemClickListener() {   	
		          @Override
		          public void onItemClick(AdapterView<?> parent, final View view,
		              int position, long id) {
		            //final String item = (String) parent.getItemAtPosition(position);
		            
		            
		            final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
		            if (device == null) {
		            	return;
		            }
		
		            //put device into storage I guess
		            textViewDeviceSelected.setText("You have selected the device: " + device.getName() + "  " + device.getAddress());
		            
		            BlueToothMethods mBlueToothMethods= new BlueToothMethods(DeviceScanActivity.this);
		            mBlueToothMethods.setHeartRateDevice(device.getName(), device.getAddress());
		            
		            /* Move this code into the exact stpot where the monitoring is done
		            final HeartRateDevice hr = new HeartRateDevice(getApplicationContext(),device.getAddress());
		            
		            
		            Runnable r = new Runnable() {
		                public void run() {
		                	 hr.getheartRate();
		                }
		            };
		
		            new Thread(r).start();
		            
		            */
		
		            if (mScanning) {
		                mBluetoothAdapter.stopLeScan(mLeScanCallback);
		                mScanning = false;
		            }           
      
                  }
          });//end of setOnItemClickListener


        
        mScanning = true;
        mBluetoothAdapter.startLeScan(mLeScanCallback);
        
	}
    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText("Unknown device");
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }
	

	   private BluetoothAdapter.LeScanCallback mLeScanCallback =
	            new BluetoothAdapter.LeScanCallback() {

	        @Override
	        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
	            runOnUiThread(new Runnable() {
	                @Override
	                public void run() {
	                	
	                	 mLeDeviceListAdapter.addDevice(device);
	                     mLeDeviceListAdapter.notifyDataSetChanged();
	                	
	                }
	            });
	        }
	    };
	    
	    
	    static class ViewHolder {
	        TextView deviceName;
	        TextView deviceAddress;
	    }


}
