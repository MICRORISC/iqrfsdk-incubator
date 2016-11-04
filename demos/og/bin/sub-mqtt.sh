#!/bin/bash

mosquitto_sub -v -t +/std/sensors/protronix/+ -q 2
