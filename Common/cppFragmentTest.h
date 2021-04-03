#include <Arduino.h>
#ifndef COMMON_HEADER
#define COMMON_HEADER
#define MESSAGE_LENGTH 6 // number of bytes in a message (not incl. start), fundamental to communication protocol
#define START_BYTE 0b00000000 // null is start byte
#define MESSAGE_HISTORY_LENGTH 3 // how many past messages you've sent that you store; do this in case of corrupted messages for re-sending
#define MESSAGE_TIMEOUT_DURATION 10
// time in ms that we wait before a message finishes
// if we wait longer than this for a message to end, things can get messy

// message structure:
// 0x00 + instruction (1B) + value (4B) + checkXOR (1B)

// instruction bit significance:
// LSb tells variable type (0 = dual int, 1 = float)

typedef struct InstructionStruct {
  byte instruction;
  float floatValue;
  int intValue1;
  int intValue2;
} instructionStruct;

// conversion of unsigned char to unsigned int takes LSB
// change these to symbolic constants
typedef enum {
  // INSERT GENERATED CONTENT HERE
} instr_code;
#endif
