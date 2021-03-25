package com.example.bluetoothrgbled_android;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class BluetoothService extends Service {
    // to enable continuous bluetooth communication regardless of what activity is open
    // additionally, meant to allow control of arduino through notif even when app is closed
    private final IBinder binder = new LocalBinder();
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier
    BluetoothSocket internalBTSocket;
    ConnectedThread internalConnectedThread;
    @Override
    public void onCreate() {
        Toast.makeText(this, "BT service started", Toast.LENGTH_SHORT).show();
    }
    /*
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "BT service started", Toast.LENGTH_SHORT).show();
    }*/
    public BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        internalBTSocket = device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        return internalBTSocket;
        // creates secure outgoing connection with BT device using UUID
    }
    public BluetoothSocket getBluetoothSocket() {
        return internalBTSocket;
    }
    public ConnectedThread createBluetoothThread() {
        internalConnectedThread = new ConnectedThread(internalBTSocket);
        internalConnectedThread.start();
        return internalConnectedThread;
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
