#!/bin/bash
set APP_BASE=..
CALL %APP_BASE%\bin\vzd-cli admin cmd -p config_ru.txt -c credentials_ru.txt -b bspReadCommand.xml
pause
