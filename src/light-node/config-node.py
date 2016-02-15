#!/usr/bin/env python
# -*- coding: utf-8; mode: python; -*-

import sys
import Ice

Ice.loadSlice("../slice/iot.ice -I/usr/share/slice -I{} --all".format(Ice.getSliceDir()))
import IoT


class Client(Ice.Application):
    def run(self, args):
        if len(args) < 3:
            return self.usage()

        if "--factory-reset" in args:
            self.factory_reset(args)

        elif "--ssid" in args:
            self.setup_wifi(args)

        elif "--restart" in args:
            self.restart_node(args)

        else:
            return self.usage()

    def usage(self):
        print ("Usage: {} <proxy> [options]\n"
               "and options include: \n"
               "     [--ssid SSID [--key KEY]]\n".format(sys.argv[0]) +
               "     [--restart]\n"
               "     [--factory-reset]")
        return 1

    def setup_wifi(self, args):
        node = self.get_node_proxy(args)
        ssid = args[args.index("--ssid") + 1]
        key = ""
        if "--key" in args:
            key = args[args.index("--key") + 1]

        print "Setting wifi for '{}'".format(node)
        node.setupWiFi(ssid, key)

    def restart_node(self, args):
        node = self.get_node_proxy(args)
        node.restart()

    def factory_reset(self, args):
        node = self.get_node_proxy(args)
        node.factoryReset()

    def get_node_proxy(self, args):
        ic = self.communicator()
        node = ic.stringToProxy(args[1])
        node = node.ice_encodingVersion(Ice.Encoding_1_0)
        return IoT.NodePrx.uncheckedCast(node)


if __name__ == '__main__':
    Client().main(sys.argv)
