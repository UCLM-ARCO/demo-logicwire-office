#!/usr/bin/env python
# -*- mode: python; coding: utf-8 -*-

import sys
import Ice
from commodity.args import parser, add_argument

Ice.loadSlice("-I/usr/share/slice/ /usr/share/slice/duo/duo_idm.ice --all")
import DUO
import IDM


class ActiveWI(DUO.IDM.Active.W):
    def __init__(self, router):
        self.router = router
        self.state = False
        self.observer = None

    def setObserver(self, addr, current):
        oid = self.addr_to_oid(addr)
        print("\nSet observer to: " + oid.name)
        self.observer = self.router.ice_identity(oid)
        self.observer = DUO.IDM.IBool.WPrx.uncheckedCast(self.observer)

    def addr_to_oid(self, addr):
        retval = ""
        for c in addr:
            retval += "{:02x}".format(ord(c))
        ic = self.router.ice_getCommunicator()
        return ic.stringToIdentity(retval)

    def notify(self):
        new_state = not self.state
        if self.observer is not None:
            print("\nSet {} on {}".format(new_state, self.observer))
            self.observer.set(new_state, [])
        self.state = new_state


class ButtonServer(Ice.Application):
    def run(self, args):
        self.ic = self.communicator()
        if not self.parse_arguments(args):
            return 1

        self.create_adapter()
        self.register_servant()
        self.announce_on_router()
        self.event_loop()

    def parse_arguments(self, args):
        add_argument("--oid", default="10:01", help="Object ID")
        try:
            self.args = parser.parse_args(args[1:])
            return True
        except SystemExit:
            return False

    def create_adapter(self):
        self.ic = self.communicator()
        self.adapter = self.ic.createObjectAdapterWithEndpoints("Adapter", "tcp")
        self.adapter.activate()

    def register_servant(self):
        self.router = self.ic.propertyToProxy("IDM.Router.Proxy")
        self.servant = ActiveWI(self.router)
        oid = self.ic.stringToIdentity(self.args.oid.replace(":", ""))
        self.servant_prx = self.adapter.add(self.servant, oid)

    def announce_on_router(self):
        self.router = IDM.NeighborDiscovery.ListenerPrx.checkedCast(self.router)
        self.router.adv(self.ic.proxyToString(self.servant_prx))

    def event_loop(self):
        print "Object ready: '{}'".format(self.servant_prx)
        self.shutdownOnInterrupt()
        self.process_kb()
        print("\rBye!")

    def process_kb(self):
        try:
            while True:
                data = raw_input("{} (Enter to change)> ".format(self.servant.state))
                self.process_input(data)
        except EOFError:
            self.communicator().shutdown()

    def process_input(self, data):
        try:
            self.servant.notify()
        except Ice.Exception, e:
            print("ERROR: could not notify servant: " + str(e))


if __name__ == "__main__":
    ButtonServer().main(sys.argv)
