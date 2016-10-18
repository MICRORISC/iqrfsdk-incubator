#!/bin/bash

/usr/bin/java -Djava.library.path=natives/x64/osgi \
	-Dlogback.configurationFile=config/cdc/logback.xml \
	-cp open-iqrf-gateway-tcpcloud-0.0.2.jar: \
	com.microrisc.opengateway.core.tests.OpenGatewayTcpCloudProtronix
