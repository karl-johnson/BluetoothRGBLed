
#include <Arduino.h>
#include <SoftwareSerial.h>
//#include "Instruction.h"
#include "GeneratedConstants.h"
#include "Utils.h"

#define RED_PIN 3
#define GRN_PIN 6
#define BLU_PIN 5
#define BTH_RX 2 // bluetooth RX - connected to TX of HC-05
#define BTH_TX 4 // bluetooth TX - connected to RX of HC-05
#define BAUD_RATE 57600

// global variables
byte inputByteArray[MESSAGE_LENGTH];
byte sentMessageHistory[MESSAGE_HISTORY_LENGTH][MESSAGE_LENGTH] = {0};
bool inputReadyFlag = false;
instructionStruct latestInstruction = {0,1,0,0};
SoftwareSerial bluetooth(BTH_RX,BTH_TX);

void setup() {
  // put your setup code here, to run once:
  Serial.begin(BAUD_RATE);
  bluetooth.begin(38400);
  // this baud rate is hardcoded b/c set in HC-05 AT+ settings
  // TODO: better solution
}

void loop() {
  updateRx(&bluetooth, inputByteArray, &inputReadyFlag);
  if(inputReadyFlag) {
    inputReadyFlag = false;

    // we got a message! let's decode it.
    int returnVal = instDecode(inputByteArray, &latestInstruction);
    if(returnVal == 2) {
      // got corrupted instruction, fuck
      Serial.println("nano got corrupted instruction lmao");
    }
    //Serial.print("S rec:");
    //Serial.println(latestInstruction.instruction);
    // if the instruction was good, parse it
    switch(latestInstruction.instruction) {
      case INST_SET_LED:
        //Serial.println("Setting LEDs:");
        // first byte of value is red
        // second byte of value is green
        // third byte is blue
        // normally use int function to extract data, but 0-255 values
        // are better handled like this (rare exception)
        ///Serial.println((int)inputByteArray[1]);
        //Serial.println((int)inputByteArray[2]);
        //Serial.println((int)inputByteArray[3]);
        analogWrite(RED_PIN, inputByteArray[1]);
        analogWrite(GRN_PIN, inputByteArray[2]);
        analogWrite(BLU_PIN, inputByteArray[3]);
        encodeAndSendInts(&bluetooth, INST_CNF_LED, latestInstruction.intValue1,
           latestInstruction.intValue2, sentMessageHistory);
        break;
      case INST_STOP_ALL:
        analogWrite(RED_PIN, 0);
        analogWrite(GRN_PIN, 0);
        analogWrite(BLU_PIN, 0);
        encodeAndSendInts(&bluetooth, INST_STPD_ALL, 0, 0, sentMessageHistory);
        while(1) {
          analogWrite(RED_PIN, 255);
          delay(500);
          analogWrite(RED_PIN, 0);
          delay(500);
          // blocking loop for now; brick arduino once this is sent
        }
        break; // this is never reached, for now
      case INST_PING_FLO:
        //Serial.println(latestInstruction.floatValue,50);
        encodeAndSendFloat(&bluetooth, INST_PONG_FLO,
          latestInstruction.floatValue, sentMessageHistory);
        break;
      case INST_PING_INT:
        encodeAndSendInts(&bluetooth, INST_PONG_INT, latestInstruction.intValue1,
          latestInstruction.intValue2, sentMessageHistory);
          break;
      default:
        //Serial.println((int) INST_SET_LED);
        Serial.println("Nano: Unknown instruction!");
        break;
    }
    inputReadyFlag = false;
  }
}
