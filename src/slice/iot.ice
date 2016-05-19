// -*- mode: c++; coding: utf-8 -*-

#include <duo/duo_idm.ice>
#include <iot/node.ice>

module IoT {

    interface IBool extends
	DUO::IDM::Active::W,
	DUO::IDM::IBool::W {};

};
