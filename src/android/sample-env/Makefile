# -*- mode: makefile-gmake; coding: utf-8 -*-

PRJDIR	 = $(shell /usr/share/arco/get-project-dir.sh)
PSDIR 	 = $(PRJDIR)/../ice.property-service-simple
PS	 = $(PSDIR)/PropertyService/server.py
PSTOOL   = $(PSDIR)/PropertyService/prop-tool.py

all:

run-ps:
	$(PS) -d --Ice.Config=server.config

run-router:
	idm-router --Ice.Config=router.config

run-button:
	./button.py --oid "1002" --Ice.Config=client.config

setup-nodes:
	$(PSTOOL) set "10:00 | children" "['10:01', '10:02']" -t stringseq

	$(PSTOOL) set "10:01 | tags" "['producer']" -t stringseq
	$(PSTOOL) set "10:01 | name" "button" -t string
	$(PSTOOL) set "10:01 | icon" "touch" -t string

	$(PSTOOL) set "10:02 | tags" "['consumer']" -t stringseq
	$(PSTOOL) set "10:02 | name" "lamp" -t string
	$(PSTOOL) set "10:02 | icon" "bulb" -t string


.PHONY: clean
clean:
	$(RM) -v *.db *~
