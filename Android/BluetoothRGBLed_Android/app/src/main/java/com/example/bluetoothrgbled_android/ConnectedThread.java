package com.example.bluetoothrgbled_android;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.SystemClock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.example.bluetoothrgbled_android.MainActivity.MESSAGE_READ;

public class ConnectedThread extends Thread {
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

    public void run(Handler mHandlerIn) {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()
        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = mmInStream.available();
                if(bytes != 0) {
                    // TODO: more robust receiving - we have a fixed message length for fucks sake!
                    SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                    bytes = mmInStream.available(); // how many bytes are ready to be read?
                    bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
                    mHandlerIn.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget(); // Send the obtained bytes to the UI activity
                }
            } catch (IOException e) {
                e.printStackTrace();

                break;
            }
        }
    }

    /* Call this from the main activity to send data to the remote device */
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

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}
