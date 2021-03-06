#include <Arduino.h>
#ifndef GLOBAL_HEADER
#define GLOBAL_HEADER
#define MESSAGE_LENGTH 6 // number of bytes in a message (not incl. start), fundamental to communication protocol
#define START_BYTE 0b00000000 // null is start byte

// time in ms that we wait before a message finishes
// if we wait longer than this for a message to end, things can get messy

// message structure:
// 0x00 + instruction (1B) + value (4B) + checkXOR (1B)

// instruction bit significance:
// LSb tells variable type (0 = dual int, 1 = float)
/*
typedef struct InstructionStruct {
  byte instruction;
  float floatValue;
  int intValue1;
  int intValue2;
} instructionStruct;
*/
// conversion of unsigned char to unsigned int takes LSB
// change these to symbolic constants
typedef enum {
  // master-sent instructions have MSb 0
  // floats have 1 as LSb
  // dual ints have 0 LSB
  INST_SET_RED = 0b00000010,
  INST_SET_GRN = 0b00000100,
  INST_SET_BLU = 0b00000110,
  INST_SET_LED = 0b00001000, // set all 3 LED colors
  // slave-sent instructions have MSb 1
  // floats have 1 as LSb
  // dual ints have 0 LSB
  INST_CNF_RED = 0b10000010,
  INST_CNF_GRN = 0b10000100,
  INST_CNF_BLU = 0b10000110,
  INST_CNF_LED = 0b10001000
} instr_code;
#endif
