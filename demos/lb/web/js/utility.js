/*******************************************************************************
 * Copyright (c) 2015 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    James Sutton - Initial Contribution
 *******************************************************************************/

/*
Eclipse Paho MQTT-JS Utility
This utility can be used to test the Eclipse Paho MQTT Javascript client.
*/

//------------------------------------------------------------------------------------
//SEZNAM RCODE
//STATUS_NO_ERROR =                 0, // No error
//ERROR_FAIL =                      1,// General fail
//ERROR_PCMD =                      2, // Incorrect PCMD
//ERROR_PNUM =                      3, // Incorrect PNUM or PCMD
//ERROR_ADDR =                      4, // Incorrect Address
//ERROR_DATA_LEN =                  5, // Incorrect Data length
//ERROR_DATA =                      6, // Incorrect Data
//ERROR_HWPROFILE =                 7,// Incorrect HW Profile ID used
//ERROR_NADR =                      8, // Incorrect NADR
//ERROR_IFACE_CUSTOM_HANDLER =      9, // Data from interface consumed by Custom DPA Handler
//ERROR_MISSING_CUSTOM_DPA_HANDLER = 10,  // Custom DPA Handler is missing
//ERROR_USER_FROM =                 0x80, // Beginning of the user code error interval
//ERROR_USER_TO =                   0xFE, // End of the user error code interval
//STATUS_CONFIRMATION =             0xFF  // Error code used to mark confirmation
//------------------------------------------------------------------------------------

// Create a client instance
client = null;
var pidled = 0;
var led ;
// called when the client connects
function onConnect(context) {
  // Once a connection has been made, make a subscription and send a message.
  console.log("Client Connected");
  console.log(context);
  var statusSpan = document.getElementById("connectionStatus");
  statusSpan.innerHTML = "Connected to: " + context.invocationContext.host + ':' + context.invocationContext.port; // + ' as ' + context.invocationContext.clientId;
  setFormEnabledState(true);
  subscribe();

}

// called when the client loses its connection
function onConnectionLost(responseObject) {
  if (responseObject.errorCode !== 0) {
    console.log("Connection Lost: "+responseObject.errorMessage);
    connect();
  }
}

// called when a message arrives
function onMessageArrived(message) {
  console.log('Message Recieved: Topic: ', message.destinationName, '. Payload: ', message.payloadString, '. QoS: ', message.qos);
  console.log(message);

  //var messageTime = new Date().toISOString();
  var messageTime = new Date().toLocaleString();
  var msg = message.payloadString;
  var obj = JSON.parse(msg);
  if(obj.e[0].n == "temperature")
  {
    //{"e":[{"n":"temperature","u":"Cel","v":24.8}],"iqrf":[{"pid":10,"dpa":"resp","nadr":1,"pnum":10,"pcmd":"get","hwpid":0,"rcode":"no_error","dpavalue":76}],"bn":"urn:dev:mid:8100543A"}
    var pom = message.destinationName; //topic name
    var res = pom.split("/");

    if( res[1] == "lp")
    {
      //lp temperature
      document.getElementById("templp").innerHTML = "TEMPERATURE 2: " +obj.e[0].v + " &deg;C<br><span style='font-size:14px;'>" + messageTime + "</span>";// + obj.e[0].u;
    }
    else
    {
      document.getElementById("temp").innerHTML = "TEMPERATURE: " +obj.e[0].v + " &deg;C<br><span style='font-size:14px;'>" + messageTime + "</span>";// + obj.e[0].u;
    }
  }

  if(obj.e[0].n == "ledr" || obj.e[0].n == "ledg")
  {

     //web response (stejne pid)
     // {"e":[{"n":"ledr","sv":"pulse"}],"iqrf":[{"pid":10,"dpa":"resp","nadr":1,"pnum":6,"pcmd":"pulse","hwpid":0,"rcode":"no_error","dpavalue":76}],"bn":"urn:dev:mid:8100543A"}
     //asynchronni response
     //{"e":[{"n":"ledr","sv":"pulse"}],"iqrf":[{"pid":12,"dpa":"resp","nadr":1,"pnum":6,"pcmd":"pulse","hwpid":0,"rcode":"no_error","dpavalue":73}],"bn":"urn:dev:mid:8100543A"}

     var pid_led_response = obj.iqrf[0].pid;
     var dpa_response     = obj.iqrf[0].dpavalue;
     var led_action       = obj.e[0].sv;

     if (obj.iqrf[0].rcode)
     {
     var rcode            = obj.iqrf[0].rcode;
     //if(dpa_response == 76)
    // {
        //web response
        //if (pidled == pid_led_response)
        //{
           // odpoved na pozadavek
           if ( rcode == "no_error" )
           {
              var pom = "img_" + obj.e[0].n;
              if (led_action == "on")
              {
                document.getElementById(pom).src = "img/" + obj.e[0].n + "-on.png";
                clearTimeout(myVar);
              }
              if (led_action == "off")
              {
                document.getElementById(pom).src = "img/" + obj.e[0].n + "-offf.png";
                clearTimeout(myVar);
              }
              if (led_action == "pulse")
              {
                 led = obj.e[0].n;
                 animation();
              }
              document.getElementById(obj.e[0].n).innerHTML = "STATUS: " + led_action + "<br>" + messageTime;
           }
           else
           {
              document.getElementById(obj.e[0].n).innerHTML = "STATUS: ERROR " + rcode + "<br>" + messageTime;
           }
        //}
     //}
     }
  }


  //if(obj.e[0].n == "ledr")
  //{
   // var pom = message.destinationName;
   // var res = pom.split("/");
  //  var count = res.length;
  //
   // var top = res[count-1];
   // switch(obj.e[0].sv) {
   // case "on":
   //      document.getElementById(top).innerHTML = "STATUS: ON<br>" + messageTime;
   //     break;
  //  case "off":
  //      document.getElementById(top).innerHTML = "STATUS: OFF<br>" + messageTime;
  //      break;
  //  case "pulse":
  //      document.getElementById(top).innerHTML = "STATUS: PULSE<br>" + messageTime;
  //      break;
  //  }

  //}
  // Insert into History Table
  var table = document.getElementById("incomingMessageTable").getElementsByTagName('tbody')[0];
  var row = table.insertRow(0);
  row.insertCell(0).innerHTML = message.destinationName;
  row.insertCell(1).innerHTML = safe_tags_regex(message.payloadString);
  row.insertCell(2).innerHTML = messageTime;
  row.insertCell(3).innerHTML = message.qos;


  if(!document.getElementById(message.destinationName)){
      var lastMessageTable = document.getElementById("lastMessageTable").getElementsByTagName('tbody')[0];
      var newlastMessageRow = lastMessageTable.insertRow(0);
      newlastMessageRow.id = message.destinationName;
      newlastMessageRow.insertCell(0).innerHTML = message.destinationName;
      newlastMessageRow.insertCell(1).innerHTML = safe_tags_regex(message.payloadString);
      newlastMessageRow.insertCell(2).innerHTML = messageTime;
      newlastMessageRow.insertCell(3).innerHTML = message.qos;

  } else {
      // Update Last Message Table
      var lastMessageRow = document.getElementById(message.destinationName);
      lastMessageRow.id = message.destinationName;
      lastMessageRow.cells[0].innerHTML = message.destinationName;
      lastMessageRow.cells[1].innerHTML = safe_tags_regex(message.payloadString);
      lastMessageRow.cells[2].innerHTML = messageTime;
      lastMessageRow.cells[3].innerHTML = message.qos;
  }

}


function connect(){
   // var hostname = document.getElementById("hostnameInput").value;
   // var port = document.getElementById("portInput").value;
   // var clientId = document.getElementById("clientIdInput").value;
    var hostname = "10.11.14.95";
    var port = "9001";
    var clientId = Math.random().toString(36).replace(/[^a-z]+/g, '').substr(0, 10);
    console.info('Connecting to Server: Hostname: ', hostname, '. Port: ', port, '. Client ID: ', clientId);
    client = new Paho.MQTT.Client(hostname, Number(port), clientId);
    // set callback handlers
    client.onConnectionLost = onConnectionLost;
    client.onMessageArrived = onMessageArrived;

    // connect the client
    client.connect({onSuccess:onConnect,
        invocationContext: {host : hostname, port: port, clientId: clientId}
    });
    var statusSpan = document.getElementById("connectionStatus");
    statusSpan.innerHTML = 'Connecting...';
}

function disconnect(){
    console.info('Disconnecting from Server');
    client.disconnect();
    var statusSpan = document.getElementById("connectionStatus");
    statusSpan.innerHTML = 'Connection - Disconnected.';
    setFormEnabledState(false);
}

// Sets various form controls to either enabled or disabled
function setFormEnabledState(enabled){
    //document.getElementById("hostnameInput").disabled = enabled;
   // document.getElementById("portInput").disabled = enabled;
   // document.getElementById("clientIdInput").disabled = enabled;
    document.getElementById("clientConnectButton").disabled = enabled;
    document.getElementById("clientDisconnectButton").disabled = !enabled;

    //document.getElementById("publishTopicInput").disabled = !enabled;
    //document.getElementById("publishQosInput").disabled = !enabled;
    //document.getElementById("publishMessageInput").disabled = !enabled;
    //document.getElementById("publishButton").disabled = !enabled;

    //document.getElementById("subscribeTopicInput").disabled = !enabled;
    //document.getElementById("subscribeQosInput").disabled = !enabled;
    //document.getElementById("subscribeButton").disabled = !enabled;
    //document.getElementById("unsubscribeButton").disabled = !enabled;

}

function publish(){
    var topic = document.getElementById("publishTopicInput").value;
    var qos = document.getElementById("publishQosInput").value;
    var message = document.getElementById("publishMessageInput").value;
    console.info('Publishing Message: Topic: ', topic, '. QoS: ' + qos + '. Message: ', message);
    message = new Paho.MQTT.Message(message);
    message.destinationName = topic;
    message.qos = Number(qos);
    client.send(message);
}

function publishiqrf(led,action){
    //var topic = document.getElementById("publishTopicInput").value;
    //var qos = document.getElementById("publishQosInput").value;
    var topic = "b827eb26c73d/actuators/leds";

    if (led == "r")
    {
      var led = "ledr";
    }
    if (led == "g")
    {
      var led = "ledg";
    }
    var qos   = 2;

    pidled = pidled + 1;

    //var message = document.getElementById("publishMessageInput").value;
    //{"e":[{"n":"ledr","sv":"pulse"}],"iqrf":[{"pid":10,"dpa":"req","nadr":1,"pnum":6,"pcmd":"pulse","hwpid":0}],"bn":"urn:dev:mid:8100543A"}
    var message = '{"e":[{"n":"' + led + '","sv":"' + action + '"}],"iqrf":[{"pid":' + pidled + ',"dpa":"req","nadr":1,"pnum":6,"pcmd":"' + action + '","hwpid":0}],"bn":"urn:dev:mid:8100543A"}';

    console.info('Publishing Message: Topic: ', topic, '. QoS: ' + qos + '. Message: ', message);
    message = new Paho.MQTT.Message(message);
    message.destinationName = topic;
    message.qos = Number(qos);
    client.send(message);
}
function subscribe(){
    //var topic = document.getElementById("subscribeTopicInput").value;
    var topic = "b827eb26c73d/sensors/thermometers";
    var topic2 = "b827eb26c73d/actuators/leds";
    var topic3 = "b827eb26c73d/asynchronous/responses";
    var topic4 = "b827eb26c73d/lp/sensors/thermometers";
    var topic5 = "b827eb26c73d/lp/actuators/leds";
    var topic6 = "b827eb26c73d/lp/asynchronous/responses";
    //var qos = document.getElementById("subscribeQosInput").value;
    var qos = 2;
    console.info('Subscribing to: Topic: ', topic, '. QoS: ', qos);
    client.subscribe(topic, {qos: Number(qos)});
    console.info('Subscribing to: Topic: ', topic2, '. QoS: ', qos);
    client.subscribe(topic2, {qos: Number(qos)});
    console.info('Subscribing to: Topic: ', topic3, '. QoS: ', qos);
    client.subscribe(topic3, {qos: Number(qos)});
    console.info('Subscribing to: Topic: ', topic3, '. QoS: ', qos);
    client.subscribe(topic4, {qos: Number(qos)});
    console.info('Subscribing to: Topic: ', topic4, '. QoS: ', qos);
    client.subscribe(topic5, {qos: Number(qos)});
    console.info('Subscribing to: Topic: ', topic5, '. QoS: ', qos);
    client.subscribe(topic6, {qos: Number(qos)});
    console.info('Subscribing to: Topic: ', topic6, '. QoS: ', qos);
}

function unsubscribe(){
   // var topic = document.getElementById("subscribeTopicInput").value;
    var topic = "b827eb26c73d/sensors/thermometers";
    console.info('Unsubscribing from ', topic);
    client.unsubscribe(topic, {
         onSuccess: unsubscribeSuccess,
         onFailure: unsubscribeFailure,
         invocationContext: {topic : topic}
     });
}


function unsubscribeSuccess(context){
    console.info('Successfully unsubscribed from ', context.invocationContext.topic);
}

function unsubscribeFailure(context){
    console.info('Failed to  unsubscribe from ', context.invocationContext.topic);
}

function clearHistory(){
    var table = document.getElementById("incomingMessageTable");
    //or use :  var table = document.all.tableid;
    for(var i = table.rows.length - 1; i > 0; i--)
    {
        table.deleteRow(i);
    }

}


// Just in case someone sends html
function safe_tags_regex(str) {
   return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

obr = -1;
cas = 1000;
var myVar;
function animation(){
if(led == "ledr")
{
  obrazky = ["img/ledr-off.png","img/ledr-on.png"];
}
else
{
  obrazky = ["img/ledg-off.png","img/ledg-on.png"];
}
if (obr+1==obrazky.length) obr=0;
else obr++;

var pom = "img_" + led;

document.getElementById(pom).src = obrazky[obr];
myVar = window.setTimeout('animation("+led+")',cas);
}




function myStopFunction() {
    clearTimeout(myVar);
}
