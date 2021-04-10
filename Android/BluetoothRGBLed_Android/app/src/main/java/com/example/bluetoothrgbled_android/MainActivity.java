package com.example.bluetoothrgbled_android;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import io.github.controlwear.virtual.joystick.android.JoystickView;

import static com.example.bluetoothrgbled_android.ConnectedThread.NEW_INSTRUCTION_IN;
import static com.example.bluetoothrgbled_android.ConnectedThread.NEW_INSTRUCTION_CORRUPTED;


public class MainActivity extends AppCompatActivity {

    BluetoothService mBluetoothService = null;
    private boolean mShouldUnbind; // to prevent unbinding when we shouldn't
    // GUI Components
    private TextView mBluetoothStatus;
    public BluetoothAdapter mBTAdapter;
    private ArrayAdapter<String> mBTArrayAdapter;
    private CheckBox mLED1;
    private boolean mBound;
    static long sentTime;
    private Handler mHandler;

    // OLD #defines for identifying shared types between calling functions
    //public final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    //public final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    //public final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status

    // BEFORE STARTING PANOGRAPH CODE!!!!!
    // TODO add interactive notification bar service for pausing/going to app/displaying progress
    // TODO add detection of BT status + detect disconnect events
    // TODO add turn-off-all command upon destroy
    // TODO more robust send/response protocol
    // TODO Exhaustive testing of bluetooth not dropping on power off etc.
    // TODO save all activity state details on switching activities

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Control LED");
        mBluetoothStatus = findViewById(R.id.bluetoothStatus);
        mLED1 = findViewById(R.id.checkboxLED1);
        mBTArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio
        JoystickView mJoystick = findViewById(R.id.hueJoystick);
        TextView mFloatSendText = findViewById(R.id.userInputFloatBox);
        Button mFloatSendButton = findViewById(R.id.floatSend);
/*
        if(mHandler == null)
            mHandler = new hHandler(this);
        else
            mHandler.setTarget(this);*/

        mHandler = new Handler(){
            public void handleMessage(Message msg){
                switch(msg.what) {
                    case NEW_INSTRUCTION_IN:
                        mBluetoothStatus.setText("Test");
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
            Toast.makeText(getApplicationContext(),
                    "Bluetooth device not found!",Toast.LENGTH_SHORT).show();
        }
        else {
            mLED1.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    if ((mLED1.isChecked())) {
                        // do nothing because joystick move should update and send color vals
                        // could also do that here if it turns out to be necessary
                    }
                    else {
                        sendColor((byte) 0,(byte) 0,(byte) 0);
                    }
                }
            });
            mJoystick.setOnMoveListener(new JoystickView.OnMoveListener() {
                @Override
                public void onMove(int angle, int strength) {
                    float[] hsv = {angle,1,strength};
                    int newColor = Color.HSVToColor(hsv);
                    Log.d("JOYSTICK_VALUE",String.format("0x%08X", newColor));
                    sendColorIfChecked((byte) ((newColor>>16) & 0xFF),
                            (byte) ((newColor>>8) & 0xFF),
                            (byte) ((newColor) & 0xFF));
                }
            }, 100);
            mFloatSendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText editText = (EditText) mFloatSendText;
                    try {
                        float floatValue = Float.parseFloat(editText.getText().toString());
                        sentTime = System.currentTimeMillis();
                        //Toast.makeText(getApplicationContext(), String.valueOf(sentTime), Toast.LENGTH_SHORT).show();
                        mBluetoothService.sendInstructionViaThread(new ArduinoInstruction(
                                GeneratedConstants.INST_PING_FLO, floatValue));
                    } catch(NumberFormatException e) {
                        Toast.makeText(getApplicationContext(), "Wrong number format!", Toast.LENGTH_SHORT).show();
                    }

                }
            });
        }
    }
/*
    // TODO: implement this non-leaky handler
    // Left off: despite calling setTarget on resume/create/everywhere, mActivity.get() always
    // returns null???? Looks like garbage collector is going haywire or something
    private static class hHandler extends Handler {
        public WeakReference<MainActivity> mActivity;

        public hHandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }
        public void setTarget(MainActivity activity) {
            mActivity.clear();
            mActivity = new WeakReference<MainActivity>(activity);
            Log.d("WEAK_REFERENCE_MADE", "Weak reference attached");
        }
        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case NEW_INSTRUCTION_IN:
                        activity.mBluetoothStatus.setText("Test");
                        activity.handleInstructionFromArduino((ArduinoInstruction) msg.obj);
                        //Log.d("MAIN_ACT_INSTR_REC", "Main activity got instruction handler.");
                        break;
                    case NEW_INSTRUCTION_CORRUPTED:
                        Toast.makeText(activity.getApplicationContext(), "Got corrupted instruction!", Toast.LENGTH_LONG).show();
                        break;
                }
            }
            else {
                Log.d("NULL_MAIN_ACTIVITY", "Main activity was null on handleMessage call");
            }
        }
    }
*/
/*
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        final EditText floatTextBox = (EditText) findViewById(R.id.userInputFloatBox);
        outState.putString("FloatBoxText",floatTextBox.getText().toString());
        Log.d("INSTANCE_STATE","Saved text:"+floatTextBox.getText().toString());
    }

    @Override
    protected  void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        final EditText floatTextBox = (EditText) findViewById(R.id.userInputFloatBox);
        floatTextBox.setText(savedInstanceState.getString("FloatBoxText"));
        Log.d("INSTANCE_STATE","Saved text:"+savedInstanceState.getString("FloatBoxText"));
    }*/

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
        if (mShouldUnbind) {
            // Release information about the service's state.
            //Toast.makeText(this, "att. unbind", Toast.LENGTH_SHORT).show();
            unbindService(connection);
            mShouldUnbind = false;
        }
        super.onPause();

    }
    public void onResume() {
        super.onResume();
        Log.d("MAIN_RESUMED","Main Service Resumed");
        Intent BTServiceIntent = new Intent(this, BluetoothService.class);
        bindService(BTServiceIntent, connection, Context.BIND_AUTO_CREATE);
        mShouldUnbind = true;
    }
    protected void onDestroy() {
        if (mShouldUnbind) {
            // Release information about the service's state.
            unbindService(connection);
            mShouldUnbind = false;
        }
        Log.d("MAIN_DESTROYED","Main Service Destroyed");
        super.onDestroy();
    }
    public void onBluetoothPress(View view) {
        Intent intent = new Intent(this, BluetoothConfigActivity.class);
        startActivity(intent);
        // no need to pass any bluetooth data, as it's all handled by Bluetooth Service
    }
    public void sendColorIfChecked(byte redValue, byte grnValue, byte bluValue) {
        if ((mLED1.isChecked())) {
            sendColor(redValue, grnValue, bluValue);
        }
    }
    public void sendColor(byte redValue, byte grnValue, byte bluValue) {
        byte[] sendColors = {redValue, grnValue, bluValue, 0x00};
        mBluetoothService.sendInstructionViaThread(new ArduinoInstruction(
                GeneratedConstants.INST_SET_LED,sendColors));
        //Toast.makeText(getApplicationContext(), "Sent value", Toast.LENGTH_SHORT).show();
        Log.d("COLORS_SENT","Values sent: "+
                String.valueOf((int)redValue)+" "+
                String.valueOf((int)grnValue)+" "+
                String.valueOf((int)bluValue));
    }
    private void handleInstructionFromArduino(ArduinoInstruction instructionIn) {
        Log.d("HEARD_INSTRUCTION","Instruction about to be handled");
        // this is where we decide what we do upon hearing each instruction from Arduino
        switch(instructionIn.instructionValue) {
            case GeneratedConstants.INST_CNF_LED:
                // set a color
                int red = ((instructionIn.intValue1) >> 8);
                int grn = ((instructionIn.intValue1) & 0x00FF);
                int blu = ((instructionIn.intValue2) >> 8);
                /*Toast.makeText(getApplicationContext(), "Got "+
                        String.valueOf(red)+" "+
                        String.valueOf(grn)+" "+
                        String.valueOf(blu), Toast.LENGTH_SHORT).show();

                 */
                //mColorFeedback.setText("TEST!");
                break;
            case GeneratedConstants.INST_STPD_ALL:
                Toast.makeText(getApplicationContext(), "Arduino confirmed stopped.", Toast.LENGTH_SHORT).show();
                break;
            case GeneratedConstants.INST_PONG_FLO:
                //timings.addSplit("Got response.");
                //timings.dumpToLog();
                long latency = System.currentTimeMillis()-sentTime;
                Toast.makeText(getApplicationContext(), String.valueOf(instructionIn.floatValue)+", "+String.valueOf(latency)+"ms", Toast.LENGTH_SHORT).show();
                break;
            case GeneratedConstants.INST_PING_INT:
                Toast.makeText(getApplicationContext(), String.valueOf(instructionIn.intValue1) + String.valueOf(instructionIn.intValue2), Toast.LENGTH_SHORT).show();
                break;
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