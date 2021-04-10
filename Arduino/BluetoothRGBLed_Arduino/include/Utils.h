#include <Arduino.h>
#include <SoftwareSerial.h>
#include "GeneratedConstants.h"
//#include "Instruction.h"
#ifndef UTILS_H
#define UTILS_H

#define MESSAGE_HISTORY_LENGTH 3
// how many past messages you've sent that you store; do this in case of corrupted messages for re-sending
#define MESSAGE_TIMEOUT_DURATION 10



typedef struct InstructionStruct {
  byte instruction;
  float floatValue;
  int intValue1;
  int intValue2;
} instructionStruct;


void updateRx(SoftwareSerial*, byte*, bool*);

byte XORbyteArray(byte*, int);

void updateMessageHistory(byte*, byte(*)[MESSAGE_LENGTH]);

int encodeAndSendFloat(SoftwareSerial*, byte, float, byte(*)[MESSAGE_LENGTH]);

int encodeAndSendInts(SoftwareSerial*, byte, int, int, byte(*)[MESSAGE_LENGTH]);

int instDecode(byte*, instructionStruct*);
#endif
