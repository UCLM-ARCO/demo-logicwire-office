#!/bin/bash
# -*- mode: sh; coding: utf-8 -*-

# This script will set the needed properties for the nodes installed
# on ITSI office: 4 lights (consumers) and 2 switches (producers).

PS_PROXY="PropertyServer -t:tcp -h pike.esi.uclm.es -p 8888"

L1=1303
L2=1305
L3=1307
L4=1309
S1=1311
S2=1312
ROOT=1313

# set consumers properties
i=1
for OBJ in $L1 $L2 $L3 $L4; do
    prop-tool -p "$PS_PROXY" -t stringseq set "$OBJ | tags" "['consumer']"
    prop-tool -p "$PS_PROXY" -t string set "$OBJ | name" "Light $i"
    prop-tool -p "$PS_PROXY" -t string set "$OBJ | icon" "lightbulb-o"
    i=$((i+1))
done

# set producers properties
prop-tool -p "$PS_PROXY" -t string set "$S1 | name" "Left Button"
prop-tool -p "$PS_PROXY" -t string set "$S2 | name" "Right Button"
for OBJ in $S1 $S2; do
    prop-tool -p "$PS_PROXY" -t stringseq set "$OBJ | tags" "['producer']"
    prop-tool -p "$PS_PROXY" -t string set "$OBJ | icon" "dot-circle-o"
done

# set a root virtual object that contains every other
CHILDREN="['$L1', '$L2', '$L3', '$L4', '$S1', '$S2']"
prop-tool -p "$PS_PROXY" -t stringseq set "$ROOT | children" "$CHILDREN"
