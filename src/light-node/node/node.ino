// -*- mode: c++; coding: utf-8 -*-

#include <ESP8266WiFi.h>
#include <ArduinoOTA.h>

#include <ESP8266mDNS.h>     // force IDE to include this
#include <EEPROM.h>          // force IDE to include this

#include <IceC.h>
#include <IceC/platforms/esp8266/TCPEndpoint.h>
#include <IceC/platforms/esp8266/debug.hpp>

#include "config.h"
#include "iot.h"

#define MAX_SSID_SIZE   32
#define MAX_WPAKEY_SIZE 64
#define MAX_PROXY_SIZE  64

#define PIN_BUTTON      0
#define PIN_LED         4
#define PIN_RELAY       5

#define DBINDEX_SSID    0
#define DBINDEX_WPAKEY  DBINDEX_SSID + MAX_SSID_SIZE
#define DBINDEX_ROUTER  DBINDEX_WPAKEY + MAX_WPAKEY_SIZE
#define DBINDEX_RELAY   DBINDEX_ROUTER + MAX_PROXY_SIZE

typedef struct {
    Ice_Object base;

    Ice_Short observer_dbindex;
    char observer_id[5];
} IoT_IBoolI;

typedef IoT_IBoolI* IoT_IBoolIPtr;

Ice_Communicator ic;
Ice_ObjectAdapter adapter;
Ice_ObjectPrx sink;

// actuators
IoT_IBoolI relay;
IoT_Node node;

const char* local_endp = "tcp -p 4455";
char base_strprx[MAX_PROXY_SIZE];
uint16_t my_idm_address_items;
IDM_Address my_idm_address;

// forward declaration here to avoid errors produced by Arduino IDE
// when generating .cpp from .ino
// (https://github.com/arduino/Arduino/issues/4088)

void
IoT_IBoolI_init(IoT_IBoolIPtr self, uint16_t observer_dbindex);

void
IoT_IBoolI_init(IoT_IBoolIPtr self, uint16_t observer_dbindex) {
    trace();

    IoT_IBool_init((IoT_IBoolPtr)self);
    IceC_Storage_get(observer_dbindex, self->observer_id, 4);

    self->observer_dbindex = observer_dbindex;
    self->observer_id[4] = 0;
}

void
DUO_IDM_IBool_WI_set(DUO_IDM_IBool_WPtr self, Ice_Bool v, IDM_Address source) {
    Serial.println("set called");

//     IoT_IBoolIPtr self = (IoT_IBoolIPtr)self_;
//     byte msg[] = {self->cmd, v};
//
//     i2c_send(msg, 2);
//
//     if (((ActiveServantPtr)self)->observer_id[0] == 0)
// 	return;
//
//     // notify event
//     Ice_ObjectPrx_ice_identity(&sink, ((ActiveServantPtr)self)->observer_id);
//     DUO_IDM_IBool_W_set(&sink, v, my_idm_address);
}

void
DUO_IDM_Active_WI_setObserver(DUO_IDM_Active_WPtr self, IDM_Address observer) {
    //     bytesToHex(self->observer_id, observer.items, 2);
    //     IceC_Storage_put(self->observer_dbindex, self->observer_id, 4);
}

void
setup() {
    Serial.begin(115200);
    IceC_Storage_begin();

    // setup gpio
    pinMode(PIN_RELAY, OUTPUT);
    pinMode(PIN_LED, OUTPUT);
    pinMode(PIN_BUTTON, INPUT);

    // connect to Wireless network
    Serial.print("\n\nConnecting to ");
    Serial.print(WIFI_SSID);

    WiFi.mode(WIFI_STA);
    WiFi.begin(WIFI_SSID, WIFI_KEY);

    int c=0;
    while (WiFi.status() != WL_CONNECTED) {
	c++;
	Serial.print(".");
 	check_buttons();

	if (c > 60) {
	    Serial.println("Connection failed! Rebooting...");
	    delay(1000);
	    ESP.restart();
	}
	delay(500);
    }

    Serial.print("\nWiFi connected, IP address: ");
    Serial.println(WiFi.localIP());

    // setup OTA procedure
    ArduinoOTA.onStart([]() {
	    Serial.println("OTA: Start");
	});

    ArduinoOTA.onProgress([](unsigned int progress, unsigned int total) {
	    Serial.printf("OTA: Progress: %u%%\r", (progress / (total / 100)));
	});

    ArduinoOTA.onEnd([]() {
	    Serial.println("\nOTA: End");
	});

    ArduinoOTA.onError([](ota_error_t error) {
	    Serial.printf("OTA Error[%u]: ", error);
	    if (error == OTA_AUTH_ERROR) Serial.println("Auth Failed");
	    else if (error == OTA_BEGIN_ERROR) Serial.println("Begin Failed");
	    else if (error == OTA_CONNECT_ERROR) Serial.println("Connect Failed");
	    else if (error == OTA_RECEIVE_ERROR) Serial.println("Receive Failed");
	    else if (error == OTA_END_ERROR) Serial.println("End Failed");
	});

    ArduinoOTA.begin();

    // setup IDM address, used as 'oid' on invocations
    String addr = IDM_ADDR;
    my_idm_address_items =
     	(strtol(addr.substring(0, 2).c_str(), NULL, 16) && 0xff) << 8 +
	(strtol(addr.substring(2, 2).c_str(), NULL, 16) && 0xff);
    my_idm_address.items = (Ice_Byte*)&my_idm_address_items;
    my_idm_address.size = 2;

    // initialize Ice and TCP endpoint
    Ice_initialize(&ic);
    TCPEndpoint_init(&ic);

    // create object adapter
    Ice_Communicator_createObjectAdapterWithEndpoints
 	(&ic, "Adapter", local_endp, &adapter);
    Ice_ObjectAdapter_activate(&adapter);

    // initialize and register actuator servant
    IoT_IBoolI_init(&relay, DBINDEX_RELAY);
    char oid[5];
    utoa((my_idm_address_items >> 8) && 0xff, oid, 16);
    utoa(my_idm_address_items && 0xff, oid + 2, 16);
    Ice_ObjectAdapter_add(&adapter, (Ice_ObjectPtr)&relay, oid);

    Serial.println("IDM node address: " IDM_ADDR);

    // create dummy proxy to router (used with each sink's identity)
    Ice_Communicator_stringToProxy(&ic, base_strprx, &sink);

    // announce idm objects on router
    announce_idm_objects();
}

// unsigned long t1 = millis();
// unsigned long t2 = t1, t3 = t1;

void
loop() {
//     // every 5 minutes
//     if (millis() - t1 > 300000) {
// 	t1 = millis();
// 	announce_idm_objects();
//     }
//
//     // every 30 seconds
//     if (millis() - t2 > 30000) {
// 	t2 = millis();
// 	send_temperature();
// 	send_ldr();
// 	send_current();
//     }
//
//     // every second
//     if (millis() - t3 > 1000) {
// 	t3 = millis();
// 	check_pir();
//     }
//
//     // as frequent as possible
//     Ice_Communicator_loopIteration(&ic);
    check_buttons();
    ArduinoOTA.handle();
}

// void
// set_leds(byte r, byte o, byte g) {
//
//     {
// 	byte msg[] = {CMD_WRITE_LED_R, r};
// 	i2c_send(msg, 2);
//     }
//
//     {
// 	byte msg[] = {CMD_WRITE_LED_O, o};
// 	i2c_send(msg, 2);
//     }
//
//     {
// 	byte msg[] = {CMD_WRITE_LED_G, g};
// 	i2c_send(msg, 2);
//     }
// }

void
check_buttons() {
    if (digitalRead(PIN_BUTTON) == 1)
 	return;

    bool erase = false;
    bool led_state = false;
    long start = millis();
    while (digitalRead(PIN_BUTTON) == 0) {
 	delay(100);

 	if (millis() - start > 3000) {
 	    erase = true;
 	    digitalWrite(PIN_LED, led_state);
	    led_state = !led_state;
 	}
    }

    if (not erase)
	return;

    Serial.println("Erasing EEPROM...");
    IceC_Storage_clear();
    Serial.println("EEPROM cleared! Restarting node...");

    digitalWrite(PIN_LED, LOW);
    delay(500);

    ESP.restart();
}

void
announce_idm_objects() {
//     Ice_ObjectPrx_ice_identity(&sink, IDM_ROUTER);
//
//     String local_addr = WiFi.localIP().toString();
//     Ice_String ice_strprx;
//     String strprx;
//
// #define ADV(oid)							\
//     strprx = String(oid) + " -o:" + local_endp + " -h " + local_addr;	\
//     Ice_String_init(ice_strprx, strprx.c_str());			\
//     IDM_NeighborDiscovery_Listener_adv(&sink, ice_strprx);
//
//     ADV(((ActiveServantPtr)&relay)->object_id);
//     ADV(((ActiveServantPtr)&led_r)->object_id);
//     ADV(((ActiveServantPtr)&led_o)->object_id);
//     ADV(((ActiveServantPtr)&led_g)->object_id);
//
//     ADV(((ActiveServantPtr)&temperature)->object_id);
//     ADV(((ActiveServantPtr)&current)->object_id);
//     ADV(((ActiveServantPtr)&pir)->object_id);
//     ADV(((ActiveServantPtr)&ldr)->object_id);
}

// // Note: sizeof(State) returns an invalid value. This may be caused by
// // alignment. Please, review the size defined in the following
// // constant if you change the struct
//
// #define State_size 8
// typedef struct State {
//     uint8_t size;
//     uint16_t current;
//     uint16_t microphone;
//     uint16_t ldr;
//     uint8_t pir;
// } State;
//
// struct State
// retrieve_state() {
//     Wire.requestFrom(ARDSENSE_ADDR, State_size);
//
//     State state;
//     unsigned long ts = millis();
//     while(!Wire.available()) {
//
// 	// timeout expired and no data received?
// 	if (millis() - ts > 2000)
// 	    return state;
//     }
//
//     state.size = Wire.read();
//     state.current = (uint16_t)Wire.read() + ((uint16_t)Wire.read() & 0xff << 8);
//     state.microphone = (uint16_t)Wire.read() + ((uint16_t)Wire.read() & 0xff << 8);
//     state.ldr = (uint16_t)Wire.read() + ((uint16_t)Wire.read() & 0xff << 8);
//     state.pir = Wire.read();
//
//     return state;
// }
//
// int
// get_temperature() {
//     // int address = 0x49;  // ADDR if ADD0 to Vcc (like on node-rs485)
//     int address = 0x48;     // ADDR if ADD0 is GND (like on node-esp266)
//     byte msb, lsb;
//     int temperature;
//
//     Wire.requestFrom(address, 2);
//
//     unsigned long ts = millis();
//     while(!Wire.available()) {
// 	// timeout expired and no data received?
// 	if (millis() - ts > 2000)
// 	    return 0;
//     }
//
//     msb = Wire.read();
//     lsb = Wire.read();
//     temperature = ((msb) << 4);
//     temperature |= (lsb >> 4);
//
//     return (int)((temperature * 0.0625) * 10);
// }
//
// void
// send_temperature() {
//     if (temperature.observer_id[0] == 0)
// 	return;
//
//     int value = get_temperature();
//     Ice_ObjectPrx_ice_identity(&sink, temperature.observer_id);
//     DUO_IDM_IInt_W_set(&sink, value, my_idm_address);
// }
//
// void
// send_current() {
//     if (current.observer_id[0] == 0)
// 	return;
//
//     int value = retrieve_state().current;
//     Ice_ObjectPrx_ice_identity(&sink, current.observer_id);
//     DUO_IDM_IInt_W_set(&sink, value, my_idm_address);
// }
//
// void
// send_ldr() {
//     if (ldr.observer_id[0] == 0)
// 	return;
//
//     int value = retrieve_state().ldr;
//     Ice_ObjectPrx_ice_identity(&sink, ldr.observer_id);
//     DUO_IDM_IInt_W_set(&sink, value, my_idm_address);
// }
//
// void
// check_pir() {
//     if (pir.observer_id[0] == 0)
// 	return;
//
//     static bool saved_state = false;
//     bool current_state = retrieve_state().pir;
//
//     if (current_state == saved_state)
// 	return;
//
//     saved_state = current_state;
//     Ice_ObjectPrx_ice_identity(&sink, pir.observer_id);
//     DUO_IDM_IBool_W_set(&sink, current_state, my_idm_address);
// }
