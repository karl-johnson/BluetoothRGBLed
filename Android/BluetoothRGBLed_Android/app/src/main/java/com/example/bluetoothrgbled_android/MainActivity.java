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
import android.graphics.Color;
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

import static com.example.bluetoothrgbled_android.ConnectedThread.NEW_INSTRUCTION_IN;
import static com.example.bluetoothrgbled_android.ConnectedThread.NEW_INSTRUCTION_CORRUPTED;


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
    private TextView mColorFeedback;
    public Handler mHandler; // Our main handler that will receive callback notifications
    //private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path

    //private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier


    // OLD #defines for identifying shared types between calling functions
    //public final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    //public final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    //public final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status


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
        mColorFeedback = (TextView)findViewById(R.id.feedbackTextView);
        mBTArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio

        //mDevicesListView = (ListView)findViewById(R.id.devicesListView);
        //mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        //mDevicesListView.setOnItemClickListener(mDeviceClickListener);




        mHandler = new Handler(){
            public void handleMessage(Message msg){
                switch(msg.what) {
                    case NEW_INSTRUCTION_IN:
                        handleInstructionFromArduino((ArduinoInstruction) msg.obj);
                        //Log.d("MAIN_ACT_INSTR_REC", "Main activity got instruction handler.");
                        break;
                    case NEW_INSTRUCTION_CORRUPTED:
                        Toast.makeText(getApplicationContext(),"Got corrupted instruction!",Toast.LENGTH_LONG).show();
                        break;
                }
/*
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
                }*/
            }
        };

        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
            mBluetoothStatus.setText(R.string.status_bt_not_found);
            Toast.makeText(getApplicationContext(),"Bluetooth device not found!",Toast.LENGTH_SHORT).show();
        }
        else {

            mLED1.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    if ((mLED1.isChecked())) {
                        sendColor((char) mRedSlider.getValue(),(char) mGrnSlider.getValue(),(char) mBluSlider.getValue());
                    }
                    else {
                        sendColor((char) 0,(char) 0,(char) 0);
                    }
                }
            });

            mRedSlider.addOnChangeListener(new Slider.OnChangeListener() {
                @Override
                public void onValueChange(Slider slider, float value, boolean fromUser) {
                    sendColorIfChecked((char) mRedSlider.getValue(),(char) mGrnSlider.getValue(),(char) mBluSlider.getValue());
                }
            });
            mGrnSlider.addOnChangeListener(new Slider.OnChangeListener() {
                @Override
                public void onValueChange(Slider slider, float value, boolean fromUser) {
                    sendColorIfChecked((char) mRedSlider.getValue(),(char) mGrnSlider.getValue(),(char) mBluSlider.getValue());
                }
            });
            mBluSlider.addOnChangeListener(new Slider.OnChangeListener() {
                @Override
                public void onValueChange(Slider slider, float value, boolean fromUser) {
                    sendColorIfChecked((char) mRedSlider.getValue(),(char) mGrnSlider.getValue(),(char) mBluSlider.getValue());
                }
            });
        }
    }

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            mBluetoothService = binder.getService();
            // not super clean, but simply pass handler to BT service to get data from thread within
            mBluetoothService.setHandler(mHandler);
            mBound = true;
            //mConnectedThread = mBluetoothService.getBluetoothThread();
            Log.d("SERVICE_CONNECTED","BT Service Connected");
            Log.d("BIND_TEST","bind fin, mbtservice != null: "+String.valueOf(mBluetoothService != null));
            /*if(mConnectedThread != null) {

                if(mConnectedThread.isAlive()) {
                    Log.d("BT_THREAD_CONNECTED","BT Thread Connected");
                }
                else{
                    //mConnectedThread.start();
                    Log.d("BT_THREAD_ALIVE","BT Thread now NOT Alive!");
                }
            }*/
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
    public void sendColorIfChecked(char redValue, char grnValue, char bluValue) {
        if ((mLED1.isChecked())) {
            sendColor(redValue, grnValue, bluValue);
        }
    }
    public void sendColor(char redValue, char grnValue, char bluValue) {
        ArduinoInstruction sendInstruction = new ArduinoInstruction();
        sendInstruction.InstructionValue = INSTRUCTION_SET_LED;
        sendInstruction.IsFloatInstruction = false;
        sendInstruction.IsFloatInstruction = false;
        sendInstruction.intValue1 = (char) (((redValue & 0x00FF) << 8) | (grnValue & 0x00FF));
        sendInstruction.intValue2 = (char) (bluValue << 8);

        //Toast.makeText(getApplicationContext(), "Got values", Toast.LENGTH_SHORT).show();
        //if (mConnectedThread != null) {//First check to make sure thread created
            //mConnectedThread.writeIntInstruction(INSTRUCTION_SET_LED, (char) (((redValue & 0x00FF) << 8) | (grnValue & 0x00FF)), (char) (bluValue << 8)); // set led to color
            mBluetoothService.sendInstructionViaThread(sendInstruction);
            //Toast.makeText(getApplicationContext(), "Sent value", Toast.LENGTH_SHORT).show();
            Log.d("COLORS_SENT","Values sent: "+String.valueOf((int)redValue)+" "+String.valueOf((int)grnValue)+" "+String.valueOf((int)bluValue));
        //}
        //else {
        //    Toast.makeText(getApplicationContext(),"No BT thread!",Toast.LENGTH_SHORT).show();
        //}
    }
    private void handleInstructionFromArduino(ArduinoInstruction instructionIn) {
        Log.d("HEARD_INSTRUCTION","Instruction about to be handled");
        // this is where we decide what each instruction from the Arduino should do
        // how to handle ask-and-respond instructions?
        switch(instructionIn.InstructionValue) {
            case INSTRUCTION_CNF_LED:
                // set a color
                Toast.makeText(getApplicationContext(), "Got response.", Toast.LENGTH_SHORT).show();
                mColorFeedback.setText("TEST!");
/*
                int red = ((instructionIn.intValue1) >> 8);
                int grn = ((instructionIn.intValue1) & 0x00FF);
                int blu = ((instructionIn.intValue2) >> 8);
                Log.d("HEARD_INSTRUCTION_COLOR",String.valueOf(red)+" "+String.valueOf(grn)+" "+String.valueOf(blu));
                mColorFeedback.setText(String.valueOf(red)+" "+String.valueOf(grn)+" "+String.valueOf(blu));
                mColorFeedback.setBackgroundColor(Color.rgb(red,grn,blu));
                // estimate luminance to change text color
                if((red+red+blu+grn+grn+grn)/6 > 128) {
                    mColorFeedback.setTextColor(Color.BLACK);
                }
                else {
                    mColorFeedback.setTextColor(Color.WHITE);
                }
                break;*/
            default:
                Toast.makeText(getApplicationContext(), "Unknown Instruction", Toast.LENGTH_SHORT).show();
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
}