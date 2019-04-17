/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends ListActivity {
    public static String POSITION = "0";
    public static long timeString;
    public LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private boolean mScanning;
    private Handler mHandler;
    public DeviceControlActivity mDeviceControlActivity;
    // public  int POSITION = 0;
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private boolean mScanComplete;
    private BluetoothLeService mBluetoothLeService;
    public FirebaseDatabase mFirebaseDatabse;
    public DatabaseReference mDatabaseReference;
    public String mLastMilkingSessionKey;
    // public long timeString;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle(R.string.title_devices);
        getLastSession();
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);


            //    if(mLeDeviceListAdapter.mLeDevices.get(0).equals(null))

           /* {
               sendIntent();

            }*/

        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        // mLeDeviceListAdapter.clear();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        // final ArrayList device = mBluetoothAdapter.getBondedDevices();
        if (device == null) return;
        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }
    /**
     private ScanCallback mScanCallback = new ScanCallback() {
    @Override
    public void onScanResult(int callbackType, ScanResult result) {
    Log.d(TAG, "onScanResult");
    processResult(result);
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
    Log.d(TAG, "onBatchScanResults: "+results.size()+" results");
    for (ScanResult result : results) {you
    processResult(result);
    }
    }

    @Override
    public void onScanFailed(int errorCode) {
    Log.w(TAG, "LE Scan Failed: "+errorCode);
    }

    private void processResult(ScanResult result) {
    Log.i(TAG, "New LE Device: " + result.getDevice().getName() + " @ " + result.getRssi());

    /*
     * Create a new beacon from the list of obtains AD structures
     * and pass it up to the main thread
     *
    TemperatureBeacon beacon = new TemperatureBeacon(result.getScanRecord(),
    result.getDevice().getAddress(),
    result.getRssi());
    mHandler.sendMessage(Message.obtain(null, 0, beacon));
    kiya bakwass he yaar wtf man camanada phir aa gya
    }
    };
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) {

            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                    if(mLeDeviceListAdapter.getCount() != 0)
                        sendIntent();

                }
            }, SCAN_PERIOD);
            //====================================
            //=======================================

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {

            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Adapter for holding devices found through scanning.
    public class LeDeviceListAdapter extends BaseAdapter {
        public ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device)
        {
            if(!mLeDevices.contains(device))
            {

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
        public BluetoothDevice getItem(int i) {
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
            } else
            {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {


                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            {

                                final String deviceName = device.getName();
                                if(deviceName != null&& deviceName.equals("MyESP32")) {
                                    mLeDeviceListAdapter.addDevice(device);
                                    mLeDeviceListAdapter.notifyDataSetChanged();
                                    // if(mScanComplete)

                                    //if(mLeDeviceListAdapter.getDevice(Integer.valueOf(POSITION)).getName() != null ) {
                                    //if(mScanComplete){
                              /*  final Intent intent = new Intent(DeviceScanActivity.this, DeviceControlActivity.class);
                               // intent.addFlags(intent.FLAG_ACTIVITY_SINGLE_TOP);
                                  //intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
                                //intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                                ///intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, mLeDeviceListAdapter.getItem(Integer.valueOf(POSITION)));
                               // intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, mLeDeviceListAdapter.getItem(Integer.valueOf(POSITION)).getAddress());

                                if (mScanning) {
                                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                                    mScanning = false;
                                }
                                startActivity(intent);
                                //  sendIntent();
                            }*//*
                            else if(mLeDeviceListAdapter.getDevice(Integer.valueOf(POSITION)).getName() == null){

                                POSITION = "0";

                            }*//*
                       }*/
                                }

                            }

                        }
                    });
                }
            };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    private void sendIntent ()
    {
        // final ArrayList<BluetoothDevice> device = (ArrayList<BluetoothDevice>) mBluetoothAdapter.getBondedDevices();
        /*Set<BluetoothDevice> device =  mBluetoothAdapter.getBondedDevices();
        for(BluetoothDevice blue : device) {
            if (blue == null) return;
            if (blue.getName().equals("CC:50:E3:9B:94:9A")) */
        Bundle bundle = new Bundle();

        ArrayList<BluetoothDevice> mDevices =(ArrayList) mLeDeviceListAdapter.mLeDevices.clone();
        bundle.putParcelableArrayList("devices", mDevices);
        BluetoothDevice device = mLeDeviceListAdapter.getDevice(0);
        final Intent intent = new Intent(DeviceScanActivity.this,DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        intent.putExtras(bundle);
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        mLeDeviceListAdapter.clear();
        startActivity(intent);


    }


    public LeDeviceListAdapter miltiConnect()
    {/*
        int i  = 0;
        while(mLeDeviceListAdapter.getDevice(i)!= null)
        {

            {

                mBluetoothLeService.connect(mLeDeviceListAdapter.getDevice(i).getAddress());


            }
            i++;
        }*/

        return mLeDeviceListAdapter;
    }
    public void getLastSession() {
        mFirebaseDatabse = FirebaseDatabase.getInstance();
        // mDatabaseReference = mFirebaseDatabse.getReference().child("messages");
        mDatabaseReference = mFirebaseDatabse.getReference().child("MilkingSession");

        Query LastSession = mDatabaseReference.orderByKey().limitToLast(1);
        LastSession.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot snap : dataSnapshot.getChildren()) {
                    MilkingSession session = snap.getValue(MilkingSession.class);
                    mLastMilkingSessionKey = snap.getKey();
                    timeString = session.getStart_Time();

                    Log.d("YES", "Samosay Par Gaye with Chai");
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }


}