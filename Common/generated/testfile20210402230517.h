// *** GENERATED BY COMMON/GENERATEHEADERS.PY *** 
// Header file to store common communication information 
#include <Arduino.h> 
#ifndef GLOBAL_HEADER
#define GLOBAL_HEADER
// ** BEGIN INSTRUCTION DEFINITIONS
typedef enum {
	// Instruction pair: led_color
	INST_SET_LED = 0b10001000,
	INST_CNF_LED = 0b00001000,
	// Instruction pair: stop_all
	INST_STOP_ALL = 0b11111110,
	INST_STPD_ALL = 0b01111110,
	// Instruction pair: test_instr
	INST_STOP_ALL = 0b10000000,
	INST_STPD_ALL = 0b00000000
} instr_code;

#define START_BYTE 0b00000000
#define MESSAGE_LENGTH 6

#endif