call java -Djava.library.path=natives/x64^
 	-Dlogback.configurationFile=config\cdc\logback.xml^
 	-cp open-iqrf-gateway-tcpcloud-0.0.1.jar;^
	com.microrisc.opengateway.core.tests.OpenGatewayTcpCloudMicrorisc
