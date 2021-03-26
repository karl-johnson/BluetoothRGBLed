package com.example.bluetoothrgbled_android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.slider.Slider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // extra name definitions
    //public static final String EXTRA_BTADAPTER = "com.example.BluetoothRGBLed.BTADAPTER";
    //public static final String EXTRA_BTSTATUS = "com.example.BluetoothRGBLed.BTSTATUS";

    BluetoothService mBluetoothService = null;
    private boolean mBound = false; // are we bound to BluetoothService?
    private boolean mShouldUnbind; // to prevent unbinding when we shouldn't
    // GUI Components
    private TextView mBluetoothStatus;
    //private TextView mReadBuffer;
    //private Button mScanBtn;
    //private Button mOffBtn;
    //private Button mListPairedDevicesBtn;
    //private Button mDiscoverBtn;
    public BluetoothAdapter mBTAdapter;
    public Set<BluetoothDevice> mPairedDevices;
    private ArrayAdapter<String> mBTArrayAdapter;
    //private ListView mDevicesListView;
    private CheckBox mLED1;
    private Slider mRedSlider;
    private Slider mGrnSlider;
    private Slider mBluSlider;

    private Handler mHandler; // Our main handler that will receive callback notifications
    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path

    //private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier


    // #defines for identifying shared types between calling functions
    public final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    public final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    public final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status
    private final static byte INSTRUCTION_SET_LED = (byte) 0b00001000;
    private final static byte INSTRUCTION_CNF_LED = (byte) 0b10001000;
    // TODO add detection of BT status + detect disconnect events
    // TODO add turn-off-all command upon destroy
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Control LED");
        mBluetoothStatus = (TextView)findViewById(R.id.bluetoothStatus);
        //mReadBuffer = (TextView) findViewById(R.id.readBuffer);
        //mScanBtn = (Button)findViewById(R.id.scan);
        //mOffBtn = (Button)findViewById(R.id.off);
        //mDiscoverBtn = (Button)findViewById(R.id.discover);
        //mListPairedDevicesBtn = (Button)findViewById(R.id.PairedBtn);
        mLED1 = (CheckBox)findViewById(R.id.checkboxLED1);
        mRedSlider = (Slider)findViewById(R.id.redSlider);
        mGrnSlider = (Slider)findViewById(R.id.grnSlider);
        mBluSlider = (Slider)findViewById(R.id.bluSlider);

        mBTArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio

        //mDevicesListView = (ListView)findViewById(R.id.devicesListView);
        //mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        //mDevicesListView.setOnItemClickListener(mDeviceClickListener);



/*
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
                    if(msg.arg1 == 1)
                        mBluetoothStatus.setText("Connected to Device: " + (String)(msg.obj));
                    else
                        mBluetoothStatus.setText("Connection Failed");
                }
            }
        };*/

        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
            mBluetoothStatus.setText(R.string.status_bt_not_found);
            Toast.makeText(getApplicationContext(),"Bluetooth device not found!",Toast.LENGTH_SHORT).show();
        }
        else {

            mLED1.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    // cast to chars b/c 2-byte integer variable type
                    char redValue = (char) mRedSlider.getValue();
                    char grnValue = (char) mGrnSlider.getValue();
                    char bluValue = (char) mBluSlider.getValue();
                    Toast.makeText(getApplicationContext(),"Got values",Toast.LENGTH_SHORT).show();
                    if(mConnectedThread != null) {//First check to make sure thread created
                        if ((mLED1.isChecked())) {
                            mConnectedThread.writeIntInstruction(INSTRUCTION_SET_LED,
                                    (char) (((redValue & 0x00FF) << 8) | (grnValue & 0x00FF)), (char) (bluValue << 8)); // set led to color
                            Toast.makeText(getApplicationContext(),"Sent value",Toast.LENGTH_SHORT).show();
                        }
                        else {
                            mConnectedThread.writeIntInstruction(INSTRUCTION_SET_LED,
                                    (char) 0x0000, (char) 0x0000); // turn led off
                            Toast.makeText(getApplicationContext(),"Sent low",Toast.LENGTH_SHORT).show();
                        }
                    }
                    else {
                        Toast.makeText(getApplicationContext(),"No BT thread!",Toast.LENGTH_SHORT).show();
                    }
                }
            });

            mRedSlider.addOnChangeListener(new Slider.OnChangeListener() {
                @Override
                public void onValueChange(Slider slider, float value, boolean fromUser) {
                    getSliderValsAndSendColor();
                }
            });
            mGrnSlider.addOnChangeListener(new Slider.OnChangeListener() {
                @Override
                public void onValueChange(Slider slider, float value, boolean fromUser) {
                    getSliderValsAndSendColor();
                }
            });
            mBluSlider.addOnChangeListener(new Slider.OnChangeListener() {
                @Override
                public void onValueChange(Slider slider, float value, boolean fromUser) {
                    getSliderValsAndSendColor();
                }
            });
            /*
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
            });*/
        }
    }

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            mBluetoothService = binder.getService();
            mBound = true;
            mConnectedThread = mBluetoothService.getBluetoothThread();
            Log.d("SERVICE_CONNECTED","BT Service Connected");
            Log.d("BIND_TEST","bind fin, mbtservice == null: "+String.valueOf(mBluetoothService == null));
            if(mConnectedThread != null) {
                Toast.makeText(getApplicationContext(), "in if here", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    public void onStart() {
        super.onStart();
        Intent BTServiceIntent = new Intent(this, BluetoothService.class);
        startService(BTServiceIntent);
        bindService(BTServiceIntent, connection, Context.BIND_AUTO_CREATE);
        mShouldUnbind = true;
    }
    public void onPause() {
        super.onPause();
        if (mShouldUnbind) {
            // Release information about the service's state.
            //Toast.makeText(this, "att. unbind", Toast.LENGTH_SHORT).show();
            unbindService(connection);
            mShouldUnbind = false;
        }
    }
    public void onResume() {

        super.onResume();
        Log.d("MAIN_RESUMED","Main Service Resumed");
        Intent BTServiceIntent = new Intent(this, BluetoothService.class);
        bindService(BTServiceIntent, connection, Context.BIND_AUTO_CREATE);
        mShouldUnbind = true;
        //mBluetoothStatus.setText
    }
    protected void onDestroy() {
        super.onDestroy();
        if (mShouldUnbind) {
            // Release information about the service's state.
            unbindService(connection);
            mShouldUnbind = false;
        }
    }
    public void onBluetoothPress(View view) {
        Intent intent = new Intent(this, BluetoothConfigActivity.class);
        startActivity(intent);
        // no need to pass any bluetooth data, as it's all handled by Bluetooth Service
    }

    public void getSliderValsAndSendColor() {
        if ((mLED1.isChecked())) {
            char redValue = (char) mRedSlider.getValue();
            char grnValue = (char) mGrnSlider.getValue();
            char bluValue = (char) mBluSlider.getValue();
            //Toast.makeText(getApplicationContext(), "Got values", Toast.LENGTH_SHORT).show();
            if (mConnectedThread != null) {//First check to make sure thread created
                mConnectedThread.writeIntInstruction(INSTRUCTION_SET_LED,
                        (char) (((redValue & 0x00FF) << 8) | (grnValue & 0x00FF)), (char) (bluValue << 8)); // set led to color
                //Toast.makeText(getApplicationContext(), "Sent value", Toast.LENGTH_SHORT).show();
                //Log.d("COLORS_SENT","Values sent: "+redValue+" "+grnValue+" "+bluValue);
            }
        }
    }
/*
    private void bluetoothOn(View view){
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothStatus.setText(R.string.bt_enabled);
            Toast.makeText(getApplicationContext(),"Bluetooth turned on",Toast.LENGTH_SHORT).show();

        }
        else{
            Toast.makeText(getApplicationContext(),"Bluetooth is already on", Toast.LENGTH_SHORT).show();
        }
    }*/

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
                mBluetoothStatus.setText(R.string.enabled);
            }
            else
                mBluetoothStatus.setText(R.string.disabled);
        }
    }
/*
    private void bluetoothOff(View view){
        mBTAdapter.disable(); // turn off
        mBluetoothStatus.setText(R.string.disabled);
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
    }*/

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
/*
    private void listPairedDevices(View view){
        mPairedDevices = mBTAdapter.getBondedDevices();
        if(mBTAdapter.isEnabled()) {
            // put it's one to the adapter
            for (BluetoothDevice device : mPairedDevices)
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

            Toast.makeText(getApplicationContext(), "Show Paired Devices", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
    }*/

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            if(!mBTAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

            mBluetoothStatus.setText(R.string.connecting);
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
                    // Establish  the Bluetooth socket connection.
                    try {
                        mBTSocket.connect();
                    } catch (IOException e) {
                        try {
                            fail = true;
                            mBTSocket.close();
                            mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                    .sendToTarget();
                        } catch (IOException e2) {
                            //insert code to deal with this
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if(!fail) {
                        mConnectedThread = mBluetoothService.createBluetoothThread();
                        mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                                .sendToTarget();
                    }
                }
            }.start();
        }
    };

/*
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connection with BT device using UUID
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.available();
                    if(bytes != 0) {
                        // TODO: more robust receiving
                        SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                        bytes = mmInStream.available(); // how many bytes are ready to be read?
                        bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget(); // Send the obtained bytes to the UI activity
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device
        public void write(String input) {
            byte[] bytes = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        public void writeIntInstruction(byte instructionByte, char value1, char value2) {
            // this assumes Java and the Arduino have same endianness, which is a mighty assumption
            byte[] value1bytes = {(byte) (value1 >> 8), (byte) value1};
            byte[] value2bytes = {(byte) (value2 >> 8), (byte) value2};
            byte xorSignature = (byte) (instructionByte ^ value1bytes[0] ^ value1bytes[1]
                                ^ value2bytes[0] ^ value2bytes[1]);
            // this is messy but it's more annoying to add a array concatenation method/library
            byte[] sendBytes = {(byte) 0x00, instructionByte, value1bytes[1], value2bytes[0],
                value2bytes[1], value2bytes[0], xorSignature};
            try {
                mmOutStream.write((byte) 0x00);
                mmOutStream.write(instructionByte);
                mmOutStream.write(value1bytes);
                mmOutStream.write(value2bytes);
                mmOutStream.write(xorSignature);
            } catch (IOException e) { }
        }

        // Call this from the main activity to shutdown the connection
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }*/
}