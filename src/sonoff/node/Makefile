# -*- mode: makefile-gmake; coding: utf-8 -*-

SHELL        := /bin/bash
BOARD         = esp8266com:esp8266:generic

# serial upload
ESPTOOL       = $$HOME/repos/esp8266/tools/esptool/esptool
PROG_PORT    ?= /dev/ttyUSB0
PROG_SPEED    = 921600

# over the air upload
ESPTOOL_OTA   = $$HOME/repos/esp8266/tools/espota.py
OTA_PORT      = 8266

BUILD_DIR     = esp8266_build
SOURCE        = node.ino
SLICE         = iot.ice

vpath %.ino node
vpath %.ice ../../slice


all: $(SLICE:%.ice=%.h) change-node build

%.h: %.ice
	slice2c -I /usr/share/slice $^

change-node:
	@if [ -z "$$ADDR" ]; then echo -e "\nERROR: ADDR is not defined!\n"; exit 1; fi
	sed -i "s/\(IDM_ADDR *\)\(.*\)/\1\"$(ADDR)\"/g" config.h

build: arduino-set-prefs
	mkdir -p $(BUILD_DIR)
	arduino -v \
	    --pref build.path=$(BUILD_DIR) \
	    --verify $(SOURCE)

upload:
	$(ESPTOOL) \
	    -vv -cd ck -cb $(PROG_SPEED) \
	    -cp $(PROG_PORT) \
	    -ca 0x00000 -cf $(BUILD_DIR)/${SOURCE:%.ino=%.cpp.bin}

ota-upload:
	@if [ -z "$$NODE_IP" ]; then echo -e "\nERROR: NODE_IP is not defined!\n"; exit 1; fi
	python $(ESPTOOL_OTA) \
	    -i $(NODE_IP) -p $(OTA_PORT) \
	    -f $(BUILD_DIR)/${SOURCE:%.ino=%.cpp.bin}

arduino-set-prefs:
	arduino --board $(BOARD) --pref custom_FlashSize=generic_1M64 --save-prefs

tty:
	@echo "Press Ctrl + 4 to exit"
	microcom -p $(PROG_PORT) -f -s 115200

PHONY: clean
clean:
	$(RM) -rv $(BUILD_DIR) *~
	$(RM) -v $(SLICE:%.ice=%.h)
