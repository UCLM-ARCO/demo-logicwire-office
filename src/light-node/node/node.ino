// -*- mode: c++; coding: utf-8 -*-

#include <ESP8266WiFi.h>
#include <ArduinoOTA.h>
#include <Ticker.h>

#include <ESP8266mDNS.h>     // force IDE to include this
#include <EEPROM.h>          // force IDE to include this

#include <IceC.h>
#include <IceC/platforms/esp8266/TCPEndpoint.h>
#include <IceC/platforms/esp8266/debug.hpp>

#include "config.h"
#include "iot.h"

#define PIN_BUTTON      0
#define PIN_LED         13
#define PIN_RELAY       12

#define MAX_SSID_SIZE   32
#define MAX_WPAKEY_SIZE 64
#define MAX_PROXY_SIZE  64

#define DBINDEX_SSID    0
#define DBINDEX_WPAKEY  DBINDEX_SSID + MAX_SSID_SIZE
#define DBINDEX_ROUTER  DBINDEX_WPAKEY + MAX_WPAKEY_SIZE
#define DBINDEX_RELAY   DBINDEX_ROUTER + MAX_PROXY_SIZE
#define DBINDEX_ADDR    DBINDEX_RELAY + 4
#define DBINDEX_STATE   DBINDEX_ADDR + 4

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
IoT_WiFiNodeAdmin node;
IoT_IBoolI relay;

const char* local_endp = "tcp -p 4455";
byte my_idm_address_items[2];
IDM_Address my_idm_address;

Ticker async;
Ticker rebooter;
bool send_advs_now = false;

bool
is_null_proxy(Ice_ObjectPrxPtr prx) {
    return ((ObjectPtr)prx)->tag != TAG_OK;
}

String
get_local_ip() {
    if (WiFi.getMode() == WIFI_AP)
	return WiFi.softAPIP().toString();
    else
	return WiFi.localIP().toString();
}

String
get_idm_address() {
    char addr[] = {0, 0, 0, 0, 0};
    IceC_Storage_get(DBINDEX_ADDR, addr, 4);

    if (addr[0] == 0)
	return IDM_ADDR;
    return addr;
}

String
get_router_idm_address() {
    {
	char addr[] = {0, 0, 0, 0, 0};
	IceC_Storage_get(DBINDEX_ROUTER, addr, 4);
	if (addr[0] != 0)
	    return addr;
    }

    // defaults to xx:01
    String addr = get_idm_address();
    addr.setCharAt(addr.length() - 1, '1');
    addr.setCharAt(addr.length() - 2, '0');
    return addr;
}

void
create_router_proxy() {
    // get proxy from local storage
    char strprx[MAX_PROXY_SIZE];
    IceC_Storage_get(DBINDEX_ROUTER, strprx, MAX_PROXY_SIZE);

    // if not available, create a default router for current network
    if (strprx[0] == 0) {
	String router_ip = get_local_ip();
	router_ip = router_ip.substring(0, router_ip.lastIndexOf('.')) + ".2";

	String router_prx = get_router_idm_address();
	router_prx = router_prx + " -o:tcp -h " + router_ip + " -p 6140";

	strcpy(strprx, router_prx.c_str());
    }

    Ice_Communicator_stringToProxy(&ic, strprx, &sink);
    Serial.printf("IDM: using router '%s'\n", strprx);
}

void
bytes_to_hex(char* dst, const byte* src, const size_t size) {
    const char* hex = "0123456789ABCDEF";

    // platform is litle-endian
    size_t i;
    for (i=0; i<size; i++) {
	dst[2*i] = hex[(*(src+i) >> 4) & 0xF];
	dst[2*i+1] = hex[*(src+i) & 0xF];
    }
    dst[2*i] = 0;
}

void
factory_reset() {
    Serial.print("\nNode: erasing EEPROM... ");
    IceC_Storage_clear();
    Serial.println("done! Restarting node...");

    digitalWrite(PIN_LED, LOW);
    delay(500);

    ESP.restart();
}

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

    factory_reset();
}

void
check_wifi_connected() {
    int c=0;
    while (WiFi.status() != WL_CONNECTED) {
    	c++;
    	Serial.print(".");
    	check_buttons();

    	if (c > 60) {
    	    Serial.println("\nWiFi: connection failed! Rebooting...");
    	    delay(1000);
    	    ESP.restart();
    	}
    	delay(500);
    }
}

void
setup_wireless() {
    // get stored wireless settings
    char ssid[MAX_SSID_SIZE];
    ssid[0] = 0;
    IceC_Storage_get(DBINDEX_SSID, ssid, MAX_SSID_SIZE);

    // if available, try to connect
    if (ssid[0] != 0) {
	char key[MAX_WPAKEY_SIZE];
	IceC_Storage_get(DBINDEX_WPAKEY, key, MAX_WPAKEY_SIZE);

	Serial.printf("WiFi: connecting to '%s'", ssid);
	WiFi.disconnect();
	WiFi.mode(WIFI_STA);
	WiFi.enableAP(false);
	WiFi.begin(ssid, key);

	check_wifi_connected();

	Serial.printf("\nWiFi: connected, IP address: ");
	Serial.println(WiFi.localIP());
    }

    // if not available, config as AP, using IDM addr for SSID
    else {
	String addr = get_idm_address();
	strcpy(ssid, ("NODE_" +  addr).c_str());
	Serial.printf("WiFi: setting up AP as '%s'\n", ssid);

	WiFi.mode(WIFI_AP);
	WiFi.enableSTA(false);
	WiFi.softAP(ssid);
    }

    Serial.printf("WiFi: ready, IP address: ");
    Serial.println(get_local_ip());
}

void
announce_idm_objects() {
    if (is_null_proxy(&sink))
	return;

    // get IDM router identity from somewhere
    String oid = get_router_idm_address();

    // recreate router's proxy
    Ice_ObjectPrx_ice_identity(&sink, oid.c_str());

    String local_addr;
    if (WiFi.getMode() == WIFI_AP)
	local_addr = WiFi.softAPIP().toString();
    else
	local_addr = WiFi.localIP().toString();

    Ice_String ice_strprx;
    String strprx;

#define ADV(OID)							\
    strprx = String(OID) + " -o:" + local_endp + " -h " + local_addr;	\
    Ice_String_init(ice_strprx, strprx.c_str());			\
    IDM_NeighborDiscovery_Listener_adv(&sink, ice_strprx);

    char srv_oid[5];
    byte idm_addr[2] = {
	my_idm_address.items[0],
	my_idm_address.items[1],
    };

    bytes_to_hex(srv_oid, idm_addr, 2);
    ADV(srv_oid);

    idm_addr[1]++;
    bytes_to_hex(srv_oid, idm_addr, 2);
    ADV(srv_oid);

    Serial.println("IDM: advertisements sent");
}

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
    Serial.printf("DUO: set state to %s\n", v ? "True" : "False");
    digitalWrite(PIN_RELAY, v ? HIGH : LOW);
    digitalWrite(PIN_LED, v ? HIGH : LOW);
    IceC_Storage_put(DBINDEX_STATE, (char*)&v, 1);

    if (((IoT_IBoolIPtr)self)->observer_id[0] == 0)
 	return;

    if (is_null_proxy(&sink))
	return;

    // notify event
    Serial.printf("DUO: notify to '%s'\n", ((IoT_IBoolIPtr)self)->observer_id);
    Ice_ObjectPrx_ice_identity(&sink, ((IoT_IBoolIPtr)self)->observer_id);
    DUO_IDM_IBool_W_set(&sink, v, my_idm_address);
}

void
DUO_IDM_Active_WI_setObserver(DUO_IDM_Active_WPtr self_, IDM_Address observer) {
    // NOTE: method signature must be equal to its declaration
    IoT_IBoolIPtr self = (IoT_IBoolIPtr)self_;

    if (observer.items[0] == 0 && observer.items[1] == 0)
	self->observer_id[0] = 0;
    else
	bytes_to_hex(self->observer_id, observer.items, 2);

    IceC_Storage_put(self->observer_dbindex, self->observer_id, 4);
    Serial.printf("DUO: set observer to '%s'\n", self->observer_id);
}

void
IoT_WiFiNodeAdminI_setupWiFi(IoT_WiFiNodeAdminPtr self, Ice_String ssid, Ice_String key) {
    trace();

    const char zero = 0;
    IceC_Storage_put(DBINDEX_SSID, ssid.value, ssid.size);
    IceC_Storage_put(DBINDEX_SSID + ssid.size, &zero, 1);
    IceC_Storage_put(DBINDEX_WPAKEY, key.value, key.size);
    IceC_Storage_put(DBINDEX_WPAKEY + key.size, &zero, 1);

    char ssid_[ssid.size + 1]; strncpy(ssid_, ssid.value, ssid.size); ssid_[ssid.size] = 0;
    char key_[key.size + 1]; strncpy(key_, key.value, key.size); key_[key.size] = 0;
    Serial.printf("WiFi: store settings, ssid: '%s', key: '%s'\n", ssid_, key_);
}

void
IoT_NodeAdminI_setIDMRouter(IoT_NodeAdminPtr self, Ice_String proxy) {
    const char zero = 0;
    IceC_Storage_put(DBINDEX_ROUTER, proxy.value, proxy.size);
    IceC_Storage_put(DBINDEX_ROUTER + proxy.size, &zero, 1);

    async.once(1, []() { create_router_proxy(); });
}

void
IoT_NodeAdminI_restart(IoT_NodeAdminPtr self) {
    Serial.println("Node: restarting...");
    async.once(1, []() { ESP.restart(); });
}

void
IoT_NodeAdminI_factoryReset(IoT_NodeAdminPtr self) {
    async.once(1, []() { factory_reset(); });
}

void
IoT_NodeAdminI_setIDMAddress(IoT_NodeAdminPtr self, Ice_String address) {
    char addr[] = {0, 0, 0, 0, 0};
    memcpy(addr, address.value, 4);
    Serial.printf("Node: setting IDM addr to '%s', then restarting...", addr);

    IceC_Storage_put(DBINDEX_ADDR, address.value, address.size);
    async.once(1, []() { ESP.restart(); });
}

void
setup() {
    Serial.begin(115200);
    Serial.printf("\n\nSYSTEM: free flash space: %d\n", ESP.getFreeSketchSpace());
    IceC_Storage_begin();

    // setup gpio
    pinMode(PIN_RELAY, OUTPUT);
    pinMode(PIN_LED, OUTPUT);
    pinMode(PIN_BUTTON, INPUT);

    // get last state from EEPROM and set it
    {
	char state = 0;
	IceC_Storage_get(DBINDEX_STATE, &state, 1);
	digitalWrite(PIN_RELAY, state ? HIGH : LOW);
	digitalWrite(PIN_LED, state ? HIGH : LOW);
    }

    // connect to Wireless network
    setup_wireless();

    // setup OTA procedure
    ArduinoOTA.onStart([]() {
	    Serial.println("OTA: start");
	});

    ArduinoOTA.onProgress([](unsigned int progress, unsigned int total) {
	    Serial.printf("OTA: progress: %u%%\r", (progress / (total / 100)));
	});

    ArduinoOTA.onEnd([]() {
	    Serial.println("\nOTA: end");
	});

    ArduinoOTA.onError([](ota_error_t error) {
	    Serial.printf("OTA error[%u]: ", error);
	    if (error == OTA_AUTH_ERROR) Serial.println("Auth Failed");
	    else if (error == OTA_BEGIN_ERROR) Serial.println("Begin Failed");
	    else if (error == OTA_CONNECT_ERROR) Serial.println("Connect Failed");
	    else if (error == OTA_RECEIVE_ERROR) Serial.println("Receive Failed");
	    else if (error == OTA_END_ERROR) Serial.println("End Failed");
	});

    ArduinoOTA.begin();

    // setup IDM address, used as 'oid' on invocations
    String addr = get_idm_address();
    Serial.printf("IDM: admin address: '%s'\n", addr.c_str());

    my_idm_address_items[0] = strtol(addr.substring(0, 2).c_str(), NULL, 16);
    my_idm_address_items[1] = strtol(addr.substring(2, 4).c_str(), NULL, 16);
    my_idm_address.items = (Ice_Byte*)&my_idm_address_items;
    my_idm_address.size = 2;

    // initialize Ice and TCP endpoint
    Ice_initialize(&ic);
    TCPEndpoint_init(&ic);

    // create object adapter
    Ice_Communicator_createObjectAdapterWithEndpoints
 	(&ic, "Adapter", local_endp, &adapter);
    Ice_ObjectAdapter_activate(&adapter);

    // initialize and register servants
    char oid[5];
    byte servant_idm_addr[2];
    servant_idm_addr[0] = my_idm_address.items[0];
    servant_idm_addr[1] = my_idm_address.items[1];

    // register admin object
    bytes_to_hex(oid, servant_idm_addr, 2);
    IoT_WiFiNodeAdmin_init(&node);
    Ice_ObjectAdapter_add(&adapter, (Ice_ObjectPtr)&node, oid);
    Serial.printf("NODE addr: '%s'\n", oid);

    // register relay object
    servant_idm_addr[1]++;
    bytes_to_hex(oid, servant_idm_addr, 2);
    IoT_IBoolI_init(&relay, DBINDEX_RELAY);
    Ice_ObjectAdapter_add(&adapter, (Ice_ObjectPtr)&relay, oid);
    Serial.printf("RELAY addr: '%s'\n", oid);

    // create dummy proxy to router and announce objects
    create_router_proxy();
    async.once(15, []() {
	    send_advs_now = true;
	    async.attach(180, []() {
		    send_advs_now = true;
		});
	});

    // reboot node 15 minutes
    rebooter.once(900, []() {
	    IoT_NodeAdminI_restart(NULL);
	});
}

void
loop() {
    if (send_advs_now) {
	send_advs_now = false;
	announce_idm_objects();
    }

    Ice_Communicator_loopIteration(&ic);
    ArduinoOTA.handle();
    check_buttons();
}
