#!/bin/bash

set -x
convert $1 -resize 64x64 -background none -gravity center -extent 64x64 $2

find . -type f -exec exiftool -overwrite_original -all= {} + >/dev/null 2>&1 || true