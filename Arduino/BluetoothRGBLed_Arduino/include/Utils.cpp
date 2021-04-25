#include <Arduino.h>
#include <SoftwareSerial.h>
#include "GeneratedConstants.h"
//#include "Instruction.h"
#include "Utils.h"


void updateRx(SoftwareSerial* serialDevice, byte* saveArray, bool* readyFlag) {
  // designed not to require global vars
  // add timeout functionality? DOING NOW!
  static bool messageInProgress = false;
  static int byteIndex = 0;
  static unsigned long messageStartMs = 0;
  if(messageInProgress) {
    if(millis() > messageStartMs + MESSAGE_TIMEOUT_DURATION) {
      // it's been too long since the message started - give up!
      byteIndex = 0;
      messageInProgress = false;
      Serial.println("Message Timeout");
    }
  }
  while (serialDevice->available()) {
    byte inByte = serialDevice->read();
    if(messageInProgress) {
      //Serial.print(inByte, HEX);
      //Serial.print(" ");
      saveArray[byteIndex] = inByte; // add byte to array
      byteIndex++;
      if(byteIndex == MESSAGE_LENGTH) {
        // if our read data is now the length of a message
        *readyFlag = true;
        byteIndex = 0; // overwrite old message - kinda dangerous
        messageInProgress = false; // message is over
      }
    }
    else if(inByte == START_BYTE) {
      // start message after start byte; don't need to include this in array
      // test test
      messageInProgress = true;
      messageStartMs = millis();
    }
    // if we're not in a message or at the start of one, discard byte
  }
}

byte XORbyteArray(byte* input, int len) {
  byte xorResult = 0;
  for(int xorIndex = 0; xorIndex < len; xorIndex++) {
    xorResult = xorResult ^ input[xorIndex];
  }
  return xorResult;
}

void updateMessageHistory(byte newMessage[], byte messageHistory[MESSAGE_HISTORY_LENGTH][MESSAGE_LENGTH]) {
  // add newMessage to bottom of message history stack (lowest index)
  // shift old message out
  for(int message_index = MESSAGE_HISTORY_LENGTH-1; message_index > 0; message_index--) {
    memcpy(messageHistory[message_index], messageHistory[message_index-1], MESSAGE_LENGTH);
  }
  // add new message to beginning
  memcpy(messageHistory[0], newMessage, MESSAGE_LENGTH);
}

int encodeAndSendFloat(SoftwareSerial* serialDevice, byte instruction,
  float value,
  byte sentMessageHistoryIn[MESSAGE_HISTORY_LENGTH][MESSAGE_LENGTH]) {
  // given an instruction enum and float value, prepare and send MESSAGE_LENGTH byte message

  // return values:
  // -1: wrong instruction type - requires float instruction (LSb = 1)
  //  0: Serial.write called successfully

  // check that instruction is one that uses a float
  if(!(instruction % 2)) { // if LSb = 0, this is for a dual int!
    return -1;
  }
  // create MESSAGE_LENGTH byte array to send
  byte messageSend[MESSAGE_LENGTH] = {0}; // don't include null start byte yet
  messageSend[0] = instruction; // put instruction in first slot
  // place float bytes in array using memcpy
  // https://stackoverflow.com/questions/21005845/how-to-get-float-in-bytes
  memcpy(messageSend+1, &value, sizeof(float)); // copy value into next bytes
  // XOR all bytes up to last to create checksum
  messageSend[MESSAGE_LENGTH-1] = XORbyteArray(messageSend,MESSAGE_LENGTH-1);
  // update message history with byte array
  updateMessageHistory(messageSend, sentMessageHistoryIn);
  // send to serial
  serialDevice->write((byte) START_BYTE); // start byte; overly verbose for clarity
  serialDevice->write(messageSend, MESSAGE_LENGTH);
  return 0;
}

int encodeAndSendInts(SoftwareSerial* serialDevice, byte instruction,
  int value1,
  int value2,
  byte sentMessageHistoryIn[MESSAGE_HISTORY_LENGTH][MESSAGE_LENGTH]) {
  // given an instruction enum and pair of 2-byte ints,
  // prepare and send MESSAGE_LENGTH byte message

  // return values:
  // -1: wrong instruction type - requires dual int instruction (LSb = 0)
  //  0: Serial.write called successfully

  // check that instruction is one that uses a float
  if(instruction % 2) { // if LSb = 1, this instruction is for a float!
    return -1;
  }
  // create MESSAGE_LENGTH byte array to send
  byte messageSend[MESSAGE_LENGTH] = {0};
  messageSend[0] = instruction; // put instruction in first slot

  // we could worry about endianness, but hopefully if I ignore it it'll go away
  memcpy(messageSend+1, &value1, sizeof(value1)); // copy value into next bytes
  memcpy(messageSend+3, &value2, sizeof(value2)); // copy value into next bytes
  // XOR all bytes up to last to create checksum
  messageSend[MESSAGE_LENGTH-1] = XORbyteArray(messageSend,MESSAGE_LENGTH-1);
  // update message history with byte array
  updateMessageHistory(messageSend, sentMessageHistoryIn);
  // send to serial
  serialDevice->write((byte) START_BYTE); // start byte
  serialDevice->write(messageSend, MESSAGE_LENGTH);
  return 0;
}

int instDecode(byte* message, instructionStruct* returnStruct) {
  // decode a MESSAGE_LENGTH-byte message (that was already read out of Serial buffer) and put results in an instruction struct

  // return values:
  // -1: [error]
  // 0: decoded dual int instruction
  // 1: decoded float instruction
  // 2: received corrupted instruction! if you get this, you should send a "resend" instruction back to slave
  if(XORbyteArray(message, MESSAGE_LENGTH)) {
    return 2;
  }
  // if message is good, decode it!
  if(message[0]%2) { // if LSB is 1, this instruction is for a float value
    // copy intruction value to address of instruction value entry in dereferenced return struct (is this kosher?)
    //memcpy(&(returnStruct->instruction), message, sizeof(byte));
    // do the same weird trick for instruction
    returnStruct->instruction = message[0];
    //returnStruct->floatValue = 4096;
    //Serial.println("FLOAT DECODED");
    returnStruct->floatValue = *((float*) (message+1));
    //returnStruct->floatValue = *((float*) &message[1]);
    Serial.println(returnStruct->floatValue);
    // interpret next 4 bytes as a float by casting pointer to second element
    // to a float pointer
    //memcpy(&(returnStruct->floatValue), message+1, sizeof(float));
    return 1;
  }
  else { // this is for a pair of ints
    // copy the data over using more memory manipulation
    //Serial.println("INT DECODED");
    // cheange out with pointer casting!!! if it works
    //memcpy(&(returnStruct->instruction), message, sizeof(byte));
    //memcpy(&(returnStruct->intValue1), message+1, sizeof(int));
    //memcpy(&(returnStruct->intValue2), message+3, sizeof(int));
    returnStruct->instruction = message[0];
    returnStruct->intValue1 = int(
        ((byte) (message[1])) << 8 |
        ((byte) (message[2])));
    returnStruct->intValue2 = int(
        ((byte) (message[3])) << 8 |
        ((byte) (message[4])));
    return 0;
  }
}
