// -*- mode: c++; coding: utf-8 -*-

#include <ESP8266WiFi.h>
#include <ArduinoOTA.h>

#define PIN_LED         13
#define PIN_PIR         4


void
check_pir() {
    bool state = digitalRead(PIN_PIR);
    digitalWrite(PIN_LED, state);
}

void
setup_wireless() {
    String ssid = "TEST-PIR";
    Serial.printf("WiFi: setting up AP as '%s'\n", ssid.c_str());

    WiFi.mode(WIFI_AP);
    WiFi.enableSTA(false);
    WiFi.softAP(ssid.c_str());

    Serial.print("WiFi: ready, IP address: ");
    Serial.println(WiFi.softAPIP());
}

void
setup() {
    Serial.begin(115200);
    Serial.printf("\n\nSYSTEM: free flash space: %d\n", ESP.getFreeSketchSpace());

    // setup gpio
    pinMode(PIN_LED, OUTPUT);
    pinMode(PIN_PIR, INPUT_PULLUP);

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
}

void
loop() {
    ArduinoOTA.handle();
    check_pir();
}
