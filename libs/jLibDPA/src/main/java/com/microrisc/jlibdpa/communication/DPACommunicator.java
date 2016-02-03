/*
 * Copyright 2016 MICRORISC s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microrisc.jlibdpa.communication;


import com.microrisc.jlibdpa.JLibDPAException;
import com.microrisc.jlibdpa.communication.receiving.DPAReceiver;
import com.microrisc.jlibdpa.communication.receiving.ListenersManager;
import com.microrisc.jlibdpa.configuration.DPAConfiguration;
import com.microrisc.jlibdpa.convertors.AbstractResponseConvertor;
import com.microrisc.jlibdpa.convertors.ConvertorFactoryMethod;
import com.microrisc.jlibdpa.timing.TimingManager;
import com.microrisc.jlibdpa.types.DPARequest;
import com.microrisc.jlibdpa.types.DPAResponse;
import com.microrisc.jlibiqrf.IQRFListener;
import com.microrisc.jlibiqrf.JLibIQRF;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SimpleJLibDPA poskytuje a implementuje základní rozhraní, kde předává data k
 * odeslání, zde se data odesílají, řídí se časování a synchronizuje se v rámci
 * vláken. Přijatá komunikace se rozlišuje a předává se do listenerů.
 *
 * @author Martin Strouhal
 */
public final class DPACommunicator implements IQRFListener {

   /** Logger. */
   private final static Logger log = LoggerFactory.getLogger(
           DPACommunicator.class);

   private final ListenersManager listenersManager;
   private final Thread sendingThread;
   private final LinkedList<CallableRequest> requestsToSend;
   private final Object sync_Sending;
   private final Object sync_LastSentRequest;
   private volatile DPARequest lastSentRequest;
   private JLibIQRF iqrf;
   private TimingManager timeManager;

   public DPACommunicator(ListenersManager listenersManager) {
      this.requestsToSend = new LinkedList<>();
      this.sync_Sending = new Object();
      this.sync_LastSentRequest = new Object();
      this.sendingThread = new SendingThread();
      this.listenersManager = listenersManager;
   }

   public void initSender(DPAConfiguration config) {
      log.debug("initSender - start: config={}, listenersManager={}",
              config, listenersManager);

      try {
         timeManager = config.getTimingManagerClass().newInstance();
      } catch (InstantiationException | IllegalAccessException ex) {
         log.error(ex.getLocalizedMessage());
         //TODO throw exception
      }
      timeManager.init(config);
      if (timeManager instanceof DPAReceiver) {
         listenersManager.registerNewListener((DPAReceiver) timeManager);
      }

      iqrf = JLibIQRF.init(config.getIQRFConfiguration());
      if (iqrf == null) {
         String txt = "IQRF layer cannot be created with specified parameters.";
         log.error(txt);
         throw new IllegalArgumentException(txt);
      }

      //TODO maybe init result collector and etc.
      sendingThread.start();
      log.debug("initSender - end");
   }

   public UUID invokeRequest(DPARequest request) {
      log.debug("invokeRequest - start: request={}", request);
      CallableRequest callableRequest = new SimpleCallableRequest(request);
      synchronized (sync_Sending) {
         requestsToSend.add(callableRequest);
      }
      UUID uid = callableRequest.getUUID();
      log.debug("invokeRequest - end: {}", uid);
      return uid;
   }


   @Override
   public void onGetIQRFData(short[] data) {
      log.debug("onGetIQRFData - start: data={}", data);
      // synchronization for actions which are depending on lastSentRequest
      synchronized (sync_LastSentRequest) {
         // identify data
         DPAIdentifier.DPAReplyMessagesTypes type = DPAIdentifier.identify(
                 lastSentRequest, data);

         // call specific listeners
         if (type == DPAIdentifier.DPAReplyMessagesTypes.RESPONSE) {
            Class<? extends AbstractResponseConvertor> convertorClasz
                    = lastSentRequest.getResponseConvertor();
            AbstractResponseConvertor convertorInstance;
            try {
               convertorInstance = getResponseConvertorInstance(convertorClasz);
            } catch (JLibDPAException ex) {
               log.warn("Received response cannot be converted. " + ex);
               return;
               //TODO send Error response
            }
            DPAResponse response = convertorInstance.convert(data);
            listenersManager.callListenerResponse(response);
         }
      }
      //TODO call confirmation listener and async
      log.debug("onGetIQRFData - end");
   }

   private AbstractResponseConvertor getResponseConvertorInstance(
           Class<? extends AbstractResponseConvertor> convertorClass)
           throws JLibDPAException {
      log.debug("getResponseConvertorInstance - start: convertorClass={}",
              convertorClass);

      Method[] methods = convertorClass.getMethods();
      for (Method method : methods) {
         Annotation annot = method.getAnnotation(ConvertorFactoryMethod.class);

         if (annot instanceof ConvertorFactoryMethod) {
            AbstractResponseConvertor convertor = null;
            try {
               convertor = (AbstractResponseConvertor) method.invoke(null, null);
            } catch (IllegalAccessException | IllegalArgumentException |
                    InvocationTargetException ex) {
               String txt = "It wasn't found method with ConvertorFactoryAnnotation "
                       + "for " + convertorClass + ". " + ex;
               log.error(txt);
               throw new JLibDPAException(txt);
            }
            log.debug("getResponseConvertorInstance - end: convertor={}",
                    convertor);
            return convertor;
         }
      }
      String txt = "It wasn't found method with ConvertorFactoryAnnotation for " + convertorClass;
      log.error(txt);
      throw new JLibDPAException(txt);
   }

   private class SendingThread extends Thread {
//TODO add logging for this class

      @Override
      public void run() {
         while (!this.isInterrupted()) {
            if (timeManager.isFree()) {
               synchronized (sync_Sending) {
                  if (requestsToSend.size() > 0) {
                     CallableRequest sendingRequest = requestsToSend.pop();
                     synchronized (sync_LastSentRequest) {
                        lastSentRequest = sendingRequest.
                                getSourceRequest();
                     }

                     // sends request to iqrf layer
                     int sendResult = iqrf.sendData(sendingRequest.getData());
                     if (sendResult != JLibIQRF.SUCCESS_OPERATION) {
                        //TODO add error
                     }

                     // start timing machine
                     timeManager.startWithUnknownTime();
                  }
               }
            } else {
               try {
                  this.sleep(timeManager.getReamingTime());
               } catch (InterruptedException ex) {
                  log.warn(ex.getMessage());
               }
            }
         }
      }
   }
}
