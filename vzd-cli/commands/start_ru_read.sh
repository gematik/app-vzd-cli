#!/bin/bash
$(dirname $0)/../bin/vzd-cli admin cmd -p config_ru.txt -c credentials_ru.txt -b bspReadCommand.xml
echo Drücken Sie eine beliebige Taste . . .
read -rn1
