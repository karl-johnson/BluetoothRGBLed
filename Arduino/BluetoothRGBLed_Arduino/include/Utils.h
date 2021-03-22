#include <Arduino.h>
#include <SoftwareSerial.h>
#include "Instruction.h"
#ifndef UTILS_H
#define UTILS_H
void updateRx(SoftwareSerial*, byte*, bool*);

byte XORbyteArray(byte*, int);

void updateMessageHistory(byte*, byte(*)[MESSAGE_LENGTH]);

int encodeAndSendFloat(SoftwareSerial*, byte, float, byte(*)[MESSAGE_LENGTH]);

int encodeAndSendInts(SoftwareSerial*, byte, int, int, byte(*)[MESSAGE_LENGTH]);

int instDecode(byte*, instructionStruct*);
#endif
