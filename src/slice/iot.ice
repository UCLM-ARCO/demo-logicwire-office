// -*- mode: c++; coding: utf-8 -*-

#include <duo/duo_idm.ice>

module IoT {

    interface IBool extends DUO::IDM::Active::W, DUO::IDM::IBool::W {};

    interface Node {
	void setupWiFi(string ssid, string key);
	void setIDMRouter(string proxy);
	void restart();
	void factoryReset();
    };
};
