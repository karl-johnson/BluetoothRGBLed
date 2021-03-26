package com.example.bluetoothrgbled_android;

public class ArduinoInstruction {
    public byte InstructionValue;
    public boolean IsFloatInstruction;
    public char intValue1;
    public char intValue2;
    public float floatValue;



    public final static byte START_BYTE = 0b00000000; // just null
    public final static int MESSAGE_LENGTH = 6;


    public ArduinoInstruction() {

    }
    public void convertBytesToInstruction(byte[] inBytes) {
        // TODO check for length
        /*
        if(inBytes.size != MESSAGE_LENGTH) {
            throw exception
        }
         */
        // TODO check XOR and throw corrupted instruction exception
        /*
        if(XORByteArray(inBytes) != 0) {
            // we have a corrupted instruction
            throw exception
        }
         */
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
        byte[] internalBytes = new byte[MESSAGE_LENGTH]; // should be initialized to 0
        internalBytes[0] = InstructionValue;
        // TODO add some checks for uninitialized things
        if(IsFloatInstruction) {
            int intBits =  Float.floatToIntBits(floatValue);
            internalBytes[1] = (byte) (intBits >> 24);
            internalBytes[2] = (byte) (intBits >> 16);
            internalBytes[3] = (byte) (intBits >> 8);
            internalBytes[4] = (byte) (intBits);
            }
        else {
            internalBytes[1] = (byte) (intValue1 >> 8);
            internalBytes[2] = (byte) (intValue1);
            internalBytes[3] = (byte) (intValue2 >> 8);
            internalBytes[4] = (byte) (intValue2);
        }
        internalBytes[5] = XORByteArray(internalBytes); // index 5 is 0 prior to this so OK
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
}
