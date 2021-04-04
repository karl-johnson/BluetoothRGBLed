package com.example.bluetoothrgbled_android;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

import static com.example.bluetoothrgbled_android.ConnectedThread.NEW_INSTRUCTION_CORRUPTED;
import static com.example.bluetoothrgbled_android.ConnectedThread.NEW_INSTRUCTION_IN;

public class BluetoothService extends Service {
    // to enable continuous bluetooth communication regardless of what activity is open
    // additionally, meant to allow control of arduino through notif even when app is closed
    private final IBinder binder = new LocalBinder();
    public Handler mmHandler;
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier
    BluetoothSocket internalBTSocket = null;
    ConnectedThread internalConnectedThread = null;
    @Override
    public void onCreate() {
        Toast.makeText(this, "BT service started", Toast.LENGTH_SHORT).show();
    }
    /*
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "BT service started", Toast.LENGTH_SHORT).show();
    }*/
    public void setHandler(Handler handlerIn) {
        mmHandler = handlerIn;
    }
    public BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        internalBTSocket = device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        return internalBTSocket;
        // creates secure outgoing connection with BT device using UUID
    }
    public BluetoothSocket getBluetoothSocket() { return internalBTSocket; }
    public ConnectedThread createBluetoothThread() throws IOException {
        // get BT thread if exists, create if not
        if(internalBTSocket != null) {
            if (internalConnectedThread == null) {
                if (mmHandler == null) {
                    Log.e("NO_HANDLER_PASSED","No handler provided to BT service.");
                }
                internalConnectedThread = new ConnectedThread(internalBTSocket, mmHandler);
                internalConnectedThread.start();
                Log.d("GET_BT_THREAD", "Created BT Thread: " + String.valueOf(internalConnectedThread != null));
                Log.d("BT_THREAD_ALIVE",String.valueOf(internalConnectedThread.isAlive()));
            }
        }
        else {
            throw new IOException("No Bluetooth Socket to Create Thread");
        }
        return internalConnectedThread;
    }
    public void sendInstructionViaThread(ArduinoInstruction instructionIn) {
        if (internalConnectedThread != null) {
            internalConnectedThread.writeArduinoInstruction(instructionIn);
        }
        else {
            Log.e("SEND_W_NO_THREAD","Tried to send with no thread!");
        }
    }
    public class LocalBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    @Override
    public void onDestroy() {
        // destroy bluetooth socket
        Toast.makeText(this, "BT service stopped", Toast.LENGTH_SHORT).show();
    }
}