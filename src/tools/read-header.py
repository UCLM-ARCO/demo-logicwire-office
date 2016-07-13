#!/usr/bin/python

import serial
import sys
import time

s = serial.Serial(port="/dev/ttyUSB0", baudrate=76800)

while True:
    if not s.inWaiting() > 0:
        time.sleep(0.05)
        continue

    sys.stdout.write(s.read(1))
