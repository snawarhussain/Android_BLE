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
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    //public static final String STATION_DATA = "Station number" ;
    private BluetoothGattCallback BluetoothGattCallback;
    private TextView mConnectionState;
    private TextView mDataField;
    private Button mWriteButton;
    private TextView mStationNumber;
    private String mDeviceName;
    private FirebaseDatabase mFirebaseDatabse;
    private DatabaseReference mDatabaseReference;
    private DatabaseReference mReferenceToData;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGatt bluetoothGatt;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private MilkingSession mLastMilkingSession;
    private String mLastMilkingSessionKey = null;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic;
    private boolean mSession_started = false;
    private boolean mSessionEnded = false;
    private boolean mSessionKeyGenereated = false;
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    public BluetoothGattCharacteristic mMilkMeterCharacteristic;

    private String mActiveSessionKey = null;
    private String mDataString = null;
    private long timeString;
    String mReceivedTag = null;
    private String mDataKey = null;
    // Code to manage Service lifecycle.
    private String mStationN = null;
    private long timeDifference;
    private Data dataReceived;
    Profile mReceivedProfileData;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        //  public final static String STATION_DATA = "com.example.bluetooth.le.STATION_DATA";

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();

            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };


    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {


                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            /**===========================================
                             final UUID CCCD_ID = UUID.fromString("000002902-0000-1000-8000-00805f9b34fb");
                             final BluetoothGattDescriptor cccd = rccp_char.getDescriptor(CCCD_ID);
                             cccd.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
                             //============================================
                             */
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);


                        }
                        return true;
                    }
                    return false;
                }
            };


    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //============================================

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

        //==========================================
        //================================

        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);
        mWriteButton = (Button) findViewById(R.id.write_btn);
        mStationNumber = (TextView) findViewById(R.id.Edit_txt);
        mWriteButton.setEnabled(false);
        //================================

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);


        mStationNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mWriteButton.setEnabled(true);
                } else {
                    mWriteButton.setEnabled(false);
                }

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mWriteButton.setEnabled(true);
                } else {
                    mWriteButton.setEnabled(false);
                }
            }


            @Override
            public void afterTextChanged(Editable editable) {

            }
        });


        mWriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mStationN = mStationNumber.getText().toString();
                mStationNumber.setText("");
                mMilkMeterCharacteristic.setValue(mStationN);
                mBluetoothLeService.mBluetoothGatt.writeCharacteristic(mMilkMeterCharacteristic);

                mBluetoothLeService.updateStationNumber(mStationN);

            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    public static long getCurrentTimeUsingDate() {

        Date date = new Date();

        long timestamp = date.getTime();

        return timestamp;
    }

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
            //mData.setValue(data);
            mDataString = data;
            long start_time = getCurrentTimeUsingDate();

            double Litters = 0.0;
            boolean match = mDataString.matches("(.)" + "(.)" + "(.)" + "(\\/)" + "([+-]?\\d*\\.\\d+)(?![-+0-9\\.])" + "(\\/)" + "(.)" + "(.)" + "(\\/)" + "(.)" +
                    "(.)");
            if (match) {
                dataReceived = new Data(mDataString);
                //==============================================================
                mSession_started = true;

                timeDifference = start_time - timeString;

                if (timeDifference >= 10800000) {

                    if (mSession_started) {

                        if (mActiveSessionKey == null) {
                            mActiveSessionKey = mDatabaseReference.push().getKey();
                            mLastMilkingSessionKey = mActiveSessionKey;

//
//                    mSessionKeyGenereated = true;
                            MilkingSession milkingSession = new MilkingSession(start_time, null, Litters);
                            mDatabaseReference.child(mActiveSessionKey).setValue(milkingSession);

/**
lastQuery  = mDatabaseReference.child(mActiveSessionKey).orderByKey().limitToLast(1);
mReferenceToData =  lastQuery.getRef().child("Data");

// mReferenceToData.
mDataKey  = mReferenceToData.push().getKey();
*/
                            UpdateDataChild(mActiveSessionKey);
                            profieUpdate();
                        }
                    }
                } else {

                    MilkingSession milkingSessionObj = new MilkingSession(timeString, null, Litters);
                    HashMap<String, Object> milkingSession = new HashMap<>();
                    milkingSession.put("total_Litters", milkingSessionObj.getTotal_Litters());


                    mDatabaseReference.child(mLastMilkingSessionKey).updateChildren(milkingSession);
                    UpdateDataChild(mLastMilkingSessionKey);
                    profieUpdate();


                }


            }

            boolean matchProfile = mDataString.matches("(.)" + "(.)" + "(.)");
            if (matchProfile) {


                mReceivedTag = mDataString;

                getProfileDatabase();



                Log.d(DISPLAY_SERVICE, "received tag(only) is set ");
            }


        }


        //================================================================
    }


    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
                if (uuid.equals("beb5483e-36e1-4688-b7f5-ea07361b26a8")) {
                    mMilkMeterCharacteristic = gattCharacteristic;


                }
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }


        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2},
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2}
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void UpdateDataChild(String key) {
        DatabaseReference ref = mDatabaseReference.child(key)
                .child("data")
                .child(dataReceived.getTag());

        HashMap<String, Object> values = new HashMap<>();
        values.put("flowRate", dataReceived.getFlowRate());
        values.put("Tag", dataReceived.getTag());
        values.put("Litters", dataReceived.getLitters());
        ref.getRef().updateChildren(values);


    }

    private void profieUpdate() {
        Profile cowProfile = new Profile(null, 00, null, 00);
        DatabaseReference profile = mFirebaseDatabse.getReference().child("profile").child(dataReceived.getTag());
        HashMap<String, Object> profileValues = new HashMap<>();
        profileValues.put("RFID", cowProfile.getRfid());
        profileValues.put("age", cowProfile.getAge());
        profileValues.put("breed", cowProfile.getBreed());
        profileValues.put("totalYield", cowProfile.getTotalYield());
        profile.updateChildren(profileValues);


    }
    private void getProfileDatabase()
    {
        DatabaseReference profile = mFirebaseDatabse.getReference().child("profile").child(mReceivedTag);
        profile.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                mReceivedProfileData = dataSnapshot.getValue(Profile.class);
                if(mReceivedProfileData!= null){
                    mMilkMeterCharacteristic.setValue(mReceivedTag);
                    mBluetoothLeService.mBluetoothGatt.writeCharacteristic(mMilkMeterCharacteristic);


                }
                else{
                    mMilkMeterCharacteristic.setValue("invalid");
                    mBluetoothLeService.mBluetoothGatt.writeCharacteristic(mMilkMeterCharacteristic);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

}

