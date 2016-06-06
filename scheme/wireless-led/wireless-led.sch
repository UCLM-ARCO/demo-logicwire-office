EESchema Schematic File Version 2
LIBS:power
LIBS:device
LIBS:transistors
LIBS:conn
LIBS:linear
LIBS:regul
LIBS:74xx
LIBS:cmos4000
LIBS:adc-dac
LIBS:memory
LIBS:xilinx
LIBS:microcontrollers
LIBS:dsp
LIBS:microchip
LIBS:analog_switches
LIBS:motorola
LIBS:texas
LIBS:intel
LIBS:audio
LIBS:interface
LIBS:digital-audio
LIBS:philips
LIBS:display
LIBS:cypress
LIBS:siliconi
LIBS:opto
LIBS:atmel
LIBS:contrib
LIBS:valves
LIBS:esp8266-12
LIBS:wireless-led-cache
EELAYER 25 0
EELAYER END
$Descr A4 11693 8268
encoding utf-8
Sheet 1 1
Title ""
Date "19 jan 2016"
Rev ""
Comp ""
Comment1 ""
Comment2 ""
Comment3 ""
Comment4 ""
$EndDescr
$Comp
L ESP866_ESP-12E U1
U 1 1 569CDF5C
P 2450 3100
F 0 "U1" H 2950 2600 60  0000 C CNN
F 1 "ESP866_ESP-12E" H 2450 3750 60  0000 C CNN
F 2 "esp8266:ESP8266-12E" H 2300 2800 60  0001 C CNN
F 3 "~" H 2300 2800 60  0000 C CNN
	1    2450 3100
	1    0    0    -1  
$EndComp
$Comp
L R R4
U 1 1 569CE394
P 4050 3000
F 0 "R4" V 4130 3000 40  0000 C CNN
F 1 "10K" V 4057 3001 40  0000 C CNN
F 2 "Resistors_SMD:R_0805_HandSoldering" V 3980 3000 30  0000 C CNN
F 3 "~" H 4050 3000 30  0000 C CNN
	1    4050 3000
	0    1    1    0   
$EndComp
$Comp
L R R5
U 1 1 569CE3A3
P 4050 3150
F 0 "R5" V 4130 3150 40  0000 C CNN
F 1 "10K" V 4057 3151 40  0000 C CNN
F 2 "Resistors_SMD:R_0805_HandSoldering" V 3980 3150 30  0000 C CNN
F 3 "~" H 4050 3150 30  0000 C CNN
	1    4050 3150
	0    1    1    0   
$EndComp
$Comp
L R R6
U 1 1 569CE3B2
P 4050 3300
F 0 "R6" V 4130 3300 40  0000 C CNN
F 1 "10K" V 4057 3301 40  0000 C CNN
F 2 "Resistors_SMD:R_0805_HandSoldering" V 3980 3300 30  0000 C CNN
F 3 "~" H 4050 3300 30  0000 C CNN
	1    4050 3300
	0    1    1    0   
$EndComp
$Comp
L R R3
U 1 1 569CE3D0
P 3650 3650
F 0 "R3" V 3730 3650 40  0000 C CNN
F 1 "1K" V 3657 3651 40  0000 C CNN
F 2 "Resistors_SMD:R_0805_HandSoldering" V 3580 3650 30  0000 C CNN
F 3 "~" H 3650 3650 30  0000 C CNN
	1    3650 3650
	1    0    0    -1  
$EndComp
$Comp
L R R2
U 1 1 569CE3DF
P 1400 2800
F 0 "R2" V 1480 2800 40  0000 C CNN
F 1 "10K" V 1407 2801 40  0000 C CNN
F 2 "Resistors_SMD:R_0805_HandSoldering" V 1330 2800 30  0000 C CNN
F 3 "~" H 1400 2800 30  0000 C CNN
	1    1400 2800
	0    -1   -1   0   
$EndComp
$Comp
L R R1
U 1 1 569CE3FD
P 1400 2600
F 0 "R1" V 1480 2600 40  0000 C CNN
F 1 "10K" V 1407 2601 40  0000 C CNN
F 2 "Resistors_SMD:R_0805_HandSoldering" V 1330 2600 30  0000 C CNN
F 3 "~" H 1400 2600 30  0000 C CNN
	1    1400 2600
	0    -1   -1   0   
$EndComp
$Comp
L SW_PUSH SW1
U 1 1 569CE40C
P 1300 2350
F 0 "SW1" H 1450 2460 50  0000 C CNN
F 1 "SW_RESET" H 1300 2270 50  0000 C CNN
F 2 "Buttons_Switches_ThroughHole:SW_PUSH_SMALL" H 1300 2350 60  0000 C CNN
F 3 "~" H 1300 2350 60  0000 C CNN
	1    1300 2350
	1    0    0    -1  
$EndComp
$Comp
L LED D1
U 1 1 569CE42A
P 4000 4000
F 0 "D1" H 4000 4100 50  0000 C CNN
F 1 "LED" H 4000 3900 50  0000 C CNN
F 2 "LEDs:LED-3MM" H 4000 4000 60  0000 C CNN
F 3 "~" H 4000 4000 60  0000 C CNN
	1    4000 4000
	-1   0    0    1   
$EndComp
Text Label 1700 3300 2    60   ~ 0
3.3Vcc
Text Label 3150 3300 0    60   ~ 0
GND
NoConn ~ 2200 4000
NoConn ~ 2300 4000
NoConn ~ 2400 4000
NoConn ~ 2500 4000
NoConn ~ 2600 4000
NoConn ~ 2700 4000
NoConn ~ 1700 2700
Wire Wire Line
	1550 2800 1700 2800
Text Label 1250 2800 2    60   ~ 0
3.3Vcc
Wire Wire Line
	1700 2600 1550 2600
Text Label 1250 2600 2    60   ~ 0
3.3Vcc
Text Label 1000 2350 2    60   ~ 0
GND
Text Label 4200 3000 0    60   ~ 0
3.3Vcc
Wire Wire Line
	3150 3000 3900 3000
Wire Wire Line
	1700 2600 1700 2350
Wire Wire Line
	1700 2350 1600 2350
Wire Wire Line
	3750 2800 3750 3000
Connection ~ 3750 3000
Text Label 4350 2800 0    60   ~ 0
GND
Wire Wire Line
	3150 3100 3750 3100
Wire Wire Line
	3750 3100 3750 3150
Wire Wire Line
	3750 3150 3900 3150
Text Label 4200 3150 0    60   ~ 0
3.3Vcc
Text Label 4200 3300 0    60   ~ 0
GND
Wire Wire Line
	3150 3200 3750 3200
Wire Wire Line
	3750 3200 3750 3300
Wire Wire Line
	3750 3300 3900 3300
NoConn ~ 1700 3200
NoConn ~ 1700 3100
NoConn ~ 1700 3000
NoConn ~ 1700 2900
Text Label 3150 2900 0    60   ~ 0
LED
Wire Wire Line
	3150 2900 3650 2900
Wire Wire Line
	3650 2900 3650 3500
Wire Wire Line
	3650 3800 3650 4000
Wire Wire Line
	3650 4000 3800 4000
Text Label 4200 4000 0    60   ~ 0
GND
NoConn ~ 3150 2800
$Comp
L CONN_01X02 P2
U 1 1 5755583F
P 1700 3850
F 0 "P2" V 1650 3850 60  0000 C CNN
F 1 "PWR" V 1750 3850 60  0000 C CNN
F 2 "Pin_Headers:Pin_Header_Straight_1x02" H 1700 3850 60  0001 C CNN
F 3 "" H 1700 3850 60  0000 C CNN
	1    1700 3850
	1    0    0    -1  
$EndComp
Text Label 1500 3900 2    60   ~ 0
GND
Text Label 1500 3800 2    60   ~ 0
3.3Vcc
$Comp
L JUMPER JP1
U 1 1 575568FD
P 4050 2800
F 0 "JP1" H 4050 2950 50  0000 C CNN
F 1 "JMP_PROG" H 4050 2720 50  0000 C CNN
F 2 "Pin_Headers:Pin_Header_Straight_1x02" H 4050 2800 50  0001 C CNN
F 3 "" H 4050 2800 50  0000 C CNN
	1    4050 2800
	1    0    0    -1  
$EndComp
NoConn ~ 3150 2600
NoConn ~ 3150 2700
$EndSCHEMATC
