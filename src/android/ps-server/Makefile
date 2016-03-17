# -*- mode: makefile-gmake; coding: utf-8 -*-

PRJDIR	 = $(shell /usr/share/arco/get-project-dir.sh)
PSDIR 	 = $(PRJDIR)/../ice.property-service-simple
PS	 = $(PSDIR)/PropertyService/server.py
PSTOOL   = $(PSDIR)/PropertyService/prop-tool.py

all:

run:
	$(PS) --Ice.Config=server.config

setup-nodes: ADDR ?= "10:00"
setup-nodes:
	$(PSTOOL) set "$(ADDR) | children" "['10:01', '10:02']"
