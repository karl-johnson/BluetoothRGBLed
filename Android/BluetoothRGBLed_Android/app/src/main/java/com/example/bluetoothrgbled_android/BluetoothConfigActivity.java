package com.example.bluetoothrgbled_android;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.UUID;

public class BluetoothConfigActivity extends AppCompatActivity {

    BluetoothService mBluetoothService;
    private boolean mBound = false; // are we bound to BluetoothService?
    private boolean mShouldUnbind; // to prevent unbinding when we shouldn't

    private TextView mBluetoothStatus;
    private TextView mBluetoothStatusColor;
    private TextView mReadBuffer;
    private Button mScanBtn;
    private Button mOffBtn;
    private Button mListPairedDevicesBtn;
    private Button mDiscoverBtn;
    private ListView mDevicesListView;
    private ArrayAdapter<String> mBTArrayAdapter;
    public Set<BluetoothDevice> mPairedDevices;
    private Handler mHandler; // Our main handler that will receive callback notifications
    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path

    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    BluetoothAdapter mBTAdapter = BluetoothAdapter.getDefaultAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_config);
        setTitle("Bluetooth Settings");

        mBluetoothStatus = (TextView)findViewById(R.id.bluetoothStatus);
        mBluetoothStatusColor = (TextView)findViewById(R.id.bluetoothStatusColor);

        mReadBuffer = (TextView) findViewById(R.id.readBuffer);
        mScanBtn = (Button)findViewById(R.id.scan);
        mOffBtn = (Button)findViewById(R.id.off);
        mDiscoverBtn = (Button)findViewById(R.id.discover);
        mListPairedDevicesBtn = (Button)findViewById(R.id.PairedBtn);

        mBTArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);

        mDevicesListView = (ListView)findViewById(R.id.devicesListView);
        mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);

        mHandler = new Handler(){
            public void handleMessage(Message msg){
                if(msg.what == MESSAGE_READ){
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    mReadBuffer.setText(readMessage);
                }

                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1) {
                        mBluetoothStatus.setText("Connected to Device: " + (String) (msg.obj));
                        mBluetoothService.connectionStatus = BluetoothService.STATUS_HALF_CONNECTED;
                        updateBluetoothStatusText(mBluetoothService.connectionStatus);
                        // we aren't fully connected until we hear arduino itself!
                    }
                    else {
                        mBluetoothStatus.setText("Connection Failed");
                        mBluetoothService.connectionStatus = BluetoothService.STATUS_DISCONNECTED;
                        updateBluetoothStatusText(mBluetoothService.connectionStatus);

                    }
                }
            }
        };
        // intent = getIntent();
        mScanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothOn(v);
            }
        });

        mOffBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                bluetoothOff(v);
            }
        });

        mListPairedDevicesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                listPairedDevices(v);
            }
        });

        mDiscoverBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                discover(v);
            }
        });

    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;

            mBluetoothService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    public void onStart() {
        super.onStart();
        Intent BTServiceIntent = new Intent(this, BluetoothService.class);
        getApplicationContext().bindService(BTServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
        mShouldUnbind = true;
        if(mBluetoothService != null) updateBluetoothStatusText(mBluetoothService.connectionStatus);
    }
    private void bluetoothOn(View view){
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothStatus.setText("Bluetooth enabled");
            Toast.makeText(getApplicationContext(),"Bluetooth turned on",Toast.LENGTH_SHORT).show();

        }
        else{
            Toast.makeText(getApplicationContext(),"Bluetooth is already on", Toast.LENGTH_SHORT).show();
        }
    }

    // Enter here after user selects "yes" or "no" to enabling radio
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data){
        super.onActivityResult(requestCode, resultCode, Data);
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                mBluetoothStatus.setText("Enabled");
            }
            else
                mBluetoothStatus.setText("Disabled");
        }
    }

    private void bluetoothOff(View view){
        mBTAdapter.disable(); // turn off
        mBluetoothStatus.setText("Bluetooth disabled");
        Toast.makeText(getApplicationContext(),"Bluetooth turned Off", Toast.LENGTH_SHORT).show();
    }
    private void discover(View view){
        // Check if the device is already discovering
        if(mBTAdapter.isDiscovering()){
            mBTAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(),"Discovery stopped",Toast.LENGTH_SHORT).show();
        }
        else{
            if(mBTAdapter.isEnabled()) {
                mBTArrayAdapter.clear(); // clear items
                mBTAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
            else{
                Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
            }
        }
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    private void listPairedDevices(View view){

        mPairedDevices = mBTAdapter.getBondedDevices();
        //Toast.makeText(this,String.valueOf(mPairedDevices.size()),Toast.LENGTH_LONG).show();
        if(mBTAdapter.isEnabled()) {
            // put it's one to the adapter
            for (BluetoothDevice device : mPairedDevices)
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

            Toast.makeText(getApplicationContext(), "Show "+String.valueOf(mPairedDevices.size())+" Paired Devices", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
    }
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connection with BT device using UUID
    }
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            if(!mBTAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

            mBluetoothStatus.setText("Connecting...");
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0,info.length() - 17);

            // Spawn a new thread to avoid blocking the GUI one
            new Thread()
            {
                public void run() {
                    boolean fail = false;
                    BluetoothDevice device = mBTAdapter.getRemoteDevice(address);
                    try {
                        mBTSocket = mBluetoothService.createBluetoothSocket(device);
                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                    // Establish the Bluetooth socket connection.
                    try {
                        mBTSocket.connect();
                    } catch (IOException e) {
                        try {
                            fail = true;
                            mBTSocket.close();
                            mHandler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                        } catch (IOException e2) {
                            //insert code to deal with this
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if(!fail) {
                        try {
                            mConnectedThread = mBluetoothService.createBluetoothThread();
                        } catch (IOException e3) {
                            Log.e("THREAD_CREATION_FAILED",e3.getMessage());
                        }
                        if(mConnectedThread == null) {
                            Toast.makeText(getBaseContext(), "Thread creation failed", Toast.LENGTH_SHORT).show();
                        }
                        mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name).sendToTarget();
                    }
                }
            }.start();
        }
    };
    private void updateBluetoothStatusText(int statusIn) {
        switch(statusIn) {
            case BluetoothService.STATUS_DISCONNECTED:
                mBluetoothStatusColor.setBackgroundColor(getResources().getColor(R.color.md_red_500));
                mBluetoothStatusColor.setText("Bluetooth Disconnected");
                break;
            case BluetoothService.STATUS_HALF_CONNECTED:
                mBluetoothStatusColor.setBackgroundColor(getResources().getColor(R.color.md_orange_500));
                mBluetoothStatusColor.setText("Bluetooth Connecting");
                break;
            case BluetoothService.STATUS_CONNECTED:
                mBluetoothStatusColor.setBackgroundColor(getResources().getColor(R.color.md_green_500));
                mBluetoothStatusColor.setText("Bluetooth Connected");
                break;
        }
    }
    protected void onDestroy() {
        super.onDestroy();
        if (mShouldUnbind) {
            // Release information about the service's state.
            getApplicationContext().unbindService(mConnection);
            mShouldUnbind = false;
        }
    }
}