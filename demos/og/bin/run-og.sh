#!/bin/bash

sudo /usr/bin/java -Djava.library.path=natives/armv7l \
	-Dlogback.configurationFile=config/logback/logback.xml \
	-cp open-gateway-0.0.1.jar: \
	com.microrisc.opengateway.OpenGateway
#	com.microrisc.opengateway.OpenGateway > og.log 2>&1
