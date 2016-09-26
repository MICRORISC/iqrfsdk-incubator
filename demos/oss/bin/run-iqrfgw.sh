#!/bin/bash

/usr/bin/java -Djava.library.path=natives/armv6hf \
	-Dlogback.configurationFile=config/cdc/logback.xml \
	-cp open-iqrf-gateway-tcpcloud-0.0.2.jar: \
	com.microrisc.opengateway.core.tests.OpenGatewayTcpCloudMicrorisc > ogtc.log 2>&1
