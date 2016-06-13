#!/bin/bash

mosquitto_sub -v -t +/sensors/protronix/+ -q 2
