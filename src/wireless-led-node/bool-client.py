#!/usr/bin/env python
# -*- coding: utf-8; mode: python; -*-

import sys
import Ice
import argparse

Ice.loadSlice("../slice/iot.ice -I/usr/share/slice -I{} --all".format(Ice.getSliceDir()))
import IoT


class Client(Ice.Application):
    def run(self, args):
        if not self.parse_args(args):
            return 1

        if self.args.cmd == "set":
            self.set_state()
        elif self.args.cmd == "set-observer":
            self.set_observer()

    def parse_args(self, args):
        parser = argparse.ArgumentParser()
        parser.add_argument("proxy", help="proxy to use")
        parser.add_argument("cmd", choices=["set", "set-observer"], help="command to run")
        parser.add_argument("params", help="params for command")

        try:
            self.args = parser.parse_args(args[1:])
            return True
        except SystemExit:
            return False

    def set_state(self):
        node = self.get_proxy()
        state = self.args.params != "0"
        node.set(state, "\xc1\xc2")

        # import time
        # for i in range(30):
        #     state = not state
        #     time.sleep(0.2)
        #     node.set(state, "\xc1\xc2")

    def set_observer(self):
        node = self.get_proxy()
        addr = self.string_to_addr(self.args.params)
        node.setObserver(addr)

    def get_proxy(self):
        ic = self.communicator()
        proxy = ic.stringToProxy(self.args.proxy)
        proxy = proxy.ice_encodingVersion(Ice.Encoding_1_0)
        print proxy
        return IoT.IBoolPrx.uncheckedCast(proxy)

    def string_to_addr(self, s):
        s = s.replace(":", "")
        return int(s[:2], 16), int(s[2:], 16)


if __name__ == '__main__':
    Client().main(sys.argv)
