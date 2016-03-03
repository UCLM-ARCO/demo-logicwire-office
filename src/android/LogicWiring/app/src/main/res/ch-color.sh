#!/bin/bash

filename=$1
color=$2
suffix=$3

if [ -z "$suffix" ]; then
    suffix="color"
fi

for d in drawable drawable-hdpi drawable-mdpi drawable-xhdpi drawable-xxhdpi drawable-xxxhdpi; do
    [ -f $d/$filename ] && {

	convert $d/$filename +level-colors "'$color'", $d/${filename/.png/_$suffix.png}
    }
done
