package com.example.bluetoothrgbled_android;

import java.io.IOException;

public class ArduinoInstruction {
    public byte InstructionValue = 0x00;
    public boolean IsFloatInstruction = false;
    public char intValue1 = 0;
    public char intValue2 = 0;
    public float floatValue = 0;



    public final static byte START_BYTE = 0b00000000; // just null
    public final static int MESSAGE_LENGTH = 6;

    public void convertBytesToInstruction(byte[] inBytes) throws CorruptedInstructionException, IOException {
        // TODO check for length

        if(inBytes.length != MESSAGE_LENGTH) {
            throw new IOException("Wrong message length: "+String.valueOf(inBytes.length));
        }
        if(XORByteArray(inBytes) != 0) {
            // we have a corrupted instruction
            throw new CorruptedInstructionException("Got Corrupted Instruction");
        }
        InstructionValue = inBytes[0];
        // hard-coded nature of our comm protocol: LSB 1 for float instruction
        IsFloatInstruction = (InstructionValue & (byte) 0x01) == 1;
        if(IsFloatInstruction) {
            // TODO endianness might be wrong, check if this works
            int intBits = inBytes[0] << 24
                    | (inBytes[1] & 0xFF) << 16
                    | (inBytes[2] & 0xFF) << 8
                    | (inBytes[3] & 0xFF);
            floatValue = Float.intBitsToFloat(intBits);
        }
        else {
            intValue1 = (char) (((inBytes[1] & 0xFF) << 8) | (inBytes[2] & 0xFF));
            intValue2 = (char) (((inBytes[3] & 0xFF) << 8) | (inBytes[4] & 0xFF));
        }
    }
    public byte[] convertInstructionToBytes() {

        byte[] internalBytes = new byte[MESSAGE_LENGTH+1]; // should be initialized to 0
        // length MESSAGE_LENGTH + 1 b/c start byte
        internalBytes[0] = 0x00;
        internalBytes[1] = InstructionValue;

        if(IsFloatInstruction) {
            int intBits =  Float.floatToIntBits(floatValue);
            internalBytes[2] = (byte) (intBits >> 24);
            internalBytes[3] = (byte) (intBits >> 16);
            internalBytes[4] = (byte) (intBits >> 8);
            internalBytes[5] = (byte) (intBits);
            }
        else {
            internalBytes[2] = (byte) (intValue1 >> 8);
            internalBytes[3] = (byte) (intValue1);
            internalBytes[4] = (byte) (intValue2 >> 8);
            internalBytes[5] = (byte) (intValue2);
        }
        internalBytes[6] = XORByteArray(internalBytes); // index 5 is 0 prior to this so OK
        return internalBytes;
    }

    private byte XORByteArray(byte[] input) {
        byte xorResult = 0;
        int len = input.length;
        for(int xorIndex = 0; xorIndex < len; xorIndex++) {
            xorResult = (byte) (xorResult ^ input[xorIndex]);
        }
        return xorResult;
    }

    public class CorruptedInstructionException extends Exception {
        public CorruptedInstructionException(String message) {
            super(message);
        }
    }
    public class MessageLengthException extends Exception {
        public MessageLengthException(String message) {
            super(message);
        }
    }
}
