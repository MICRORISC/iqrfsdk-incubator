#!/bin/bash

#spi
#sudo /usr/bin/java -Djava.library.path=natives/armv7l \

#cdc
sudo /usr/bin/java -Djava.library.path=natives/armhf/osgi \
	-Dlogback.configurationFile=config/logback/logback.xml \
	-cp open-gateway-0.1.0.jar: \
	com.microrisc.opengateway.apps.monitoring.OpenGatewayO2ITSApp

#	com.microrisc.opengateway.apps.monitoring.OpenGatewayO2ITSApp > oga.log 2>&1
#	com.microrisc.opengateway.apps.monitoring.OpenGatewayIntelimentsApp
#	com.microrisc.opengateway.apps.monitoring.OpenGatewayIntelimentsApp > oga.log 2>&1
