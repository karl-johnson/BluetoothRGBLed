package com.example.bluetoothrgbled_android;

import android.app.Service;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

public class BluetoothService extends Service {
    // to enable continuous bluetooth communication regardless of what activity is open
    // additionally, meant to allow control of arduino through notif even when app is closed
    @Override
    public void onCreate() {



    }
    @Override
    public int onStartCommand(BluetoothSocket socket) {
        Toast.makeText(this, "BT service starting", Toast.LENGTH_SHORT).show();
        // create new thread
        ConnectedThread bluetoothThread = new ConnectedThread(socket);
        bluetoothThread.start();
        // TODO: functionality to access socket
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onDestroy() {
        // destroy bluetooth socket

        Toast.makeText(this, "BT service stopped", Toast.LENGTH_SHORT).show();

    }

}
