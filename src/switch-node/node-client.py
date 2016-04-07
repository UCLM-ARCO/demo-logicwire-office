#!/usr/bin/env python
# -*- coding: utf-8; mode: python; -*-

import sys
import Ice

Ice.loadSlice("../slice/iot.ice -I/usr/share/slice -I{} --all".format(Ice.getSliceDir()))
import IoT


class Client(Ice.Application):
    def run(self, args):
        if len(args) < 4:
            return self.usage()

        if "--factory-reset" in args:
            self.factory_reset(args)

        elif "--ssid" in args:
            self.setup_wifi(args)

        elif "--restart" in args:
            self.restart_node(args)

        elif "--set-observer" in args:
            self.set_observer(args)

        elif "--set-router" in args:
            self.set_router(args)

        else:
            return self.usage()

    def usage(self):
        print ("Usage: {} <idm-addr> <host-ip> [options]\n"
               "and options include: \n"
               "     [--ssid SSID [--key KEY]]\n".format(sys.argv[0]) +
               "     [--restart]\n"
               "     [--factory-reset]\n"
               "     [--set-observer <1|2> ADDR]\n"
               "     [--set-router PROXY]")
        return 1

    def setup_wifi(self, args):
        node = self.get_config_proxy(args)
        ssid = args[args.index("--ssid") + 1]
        key = ""
        if "--key" in args:
            key = args[args.index("--key") + 1]

        print "Setting wifi for '{}'".format(node)
        node.setupWiFi(ssid, key)

    def restart_node(self, args):
        node = self.get_config_proxy(args)
        node.restart()

    def factory_reset(self, args):
        node = self.get_config_proxy(args)
        node.factoryReset()

    def set_observer(self, args):
        addr = args[args.index("--set-observer") + 1]
        addr = self.string_to_addr(addr)

        node = self.get_proxy(args)
        node = IoT.IBoolPrx.uncheckedCast(node)
        node.setObserver(addr)

    def set_router(self, args):
        node = self.get_config_proxy(args)
        router = args[args.index("--set-router") + 1]
        node.setIDMRouter(router)

    def get_config_proxy(self, args):
        proxy = self.get_proxy(args)
        return IoT.NodePrx.uncheckedCast(proxy)

    def get_proxy(self, args):
        ic = self.communicator()
        addr = args[1].replace(":", "")
        host = args[2]
        proxy = "{} -e 1.0 -o:tcp -h {} -p 4455".format(addr, host)
        return ic.stringToProxy(proxy)

    def string_to_addr(self, s):
        s = s.replace(":", "")
        return int(s[:2], 16), int(s[2:], 16)


if __name__ == '__main__':
    Client().main(sys.argv)
