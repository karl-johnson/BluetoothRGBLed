package com.example.bluetoothrgbled_android;

public class InstructionConstants {
    public final static byte START_BYTE = 0b00000000;
    public final static int MESSAGE_LENGTH = 6;

    // phone to arduino instructions
    public final static byte INSTRUCTION_SET_LED = (byte) 0b00001000;

    // arduino to phone instructions
    public final static byte INSTRUCTION_CNF_LED = (byte) 0b10001000;
}
