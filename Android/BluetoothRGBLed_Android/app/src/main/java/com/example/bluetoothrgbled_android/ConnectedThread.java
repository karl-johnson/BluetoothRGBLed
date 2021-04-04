package com.example.bluetoothrgbled_android;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
//import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

//import static com.example.bluetoothrgbled_android.MainActivity.MESSAGE_READ;
//public final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
//public final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update

public class ConnectedThread extends Thread {

    public final static int NEW_INSTRUCTION_IN = 1; // used to identify adding bluetooth names
    public final static int NEW_INSTRUCTION_CORRUPTED = 2; // used in bluetooth handler to identify message update

    private final BluetoothSocket mmSocket;
    private final Handler mmHandler;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    //private final static byte START_BYTE = 0b00000000; // just null
    //private final static int MESSAGE_LENGTH = 6;

    private boolean messageInProgress = false; // keep track of whether message is in progress
    private int byteIndex = 0; // keep track of location in message
    private byte[] saveArray = new byte[GeneratedConstants.MESSAGE_LENGTH];

    public ConnectedThread(BluetoothSocket socket, Handler mHandlerIn) {
        mmSocket = socket;
        mmHandler = mHandlerIn;
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
        //byte[] buffer = new byte[1024];  // buffer store for the stream
        //int bytes; // bytes returned from read()
        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // new code
                // look for start character, start string once get, end string once right length
                while(mmInStream.available() > 0) {
                    //Log.d("BYTE","byte from Arduino");
                    byte inByte = (byte) mmInStream.read();
                    if(messageInProgress) {
                        saveArray[byteIndex] = inByte; // add byte to array
                        byteIndex++;
                        if(byteIndex == GeneratedConstants.MESSAGE_LENGTH) {
                            //Log.d("MESSAGE_COMPLETE","Got to Message Completion");
                            // if our read data is now the length of a message
                            // decode instruction
                            ArduinoInstruction newInstruction = new ArduinoInstruction();
                            try {
                                newInstruction.convertBytesToInstruction(saveArray);
                                mmHandler.obtainMessage(NEW_INSTRUCTION_IN, newInstruction).sendToTarget();
                                Log.d("SENT","Sent instruction handler");
                            } catch (ArduinoInstruction.CorruptedInstructionException e) {
                                mmHandler.obtainMessage(NEW_INSTRUCTION_CORRUPTED).sendToTarget();
                            } catch (IOException e2) {
                                Log.e("BAD_ENC_MESSAGE_LENGTH",e2.getMessage());
                            }
                            byteIndex = 0; // overwrite old message
                            messageInProgress = false; // message is over
                        }
                    }
                    else if(inByte == GeneratedConstants.START_BYTE) {
                        // start message after start byte; don't need to include this in array
                        messageInProgress = true;
                    }
                }
                /*
                //old code
                // Read from the InputStream
                bytes = mmInStream.available();
                if(bytes != 0) {
                    SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                    bytes = mmInStream.available(); // how many bytes are ready to be read?
                    bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
                    mHandlerIn.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget(); // Send the obtained bytes to the UI activity
                }*/
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public void writeArduinoInstruction(ArduinoInstruction inputInstruction) {
        //Log.d("SENDING_INSTRUCTION", "Attempting to write instruction bytes");
        byte[] sendBytes = inputInstruction.convertInstructionToBytes();
        Log.d("BYTES_SENT",bytesToHex(sendBytes));
        try {
            mmOutStream.write(sendBytes);
        } catch (IOException e) {
            Log.e("WRITE_INSTR_FAILED", "Attempt to write instruction bytes failed");
        }
    }

    // temp function to print byte array in hex

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes();
    public static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}
