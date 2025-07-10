<filename>api-server/vscode-codeql-starter/ql/java/ql/test/query-tests/security/CWE-502/ServiceImpl.java<fim_prefix>

//+----------------------------------------------------------------------------+
//|  Copyright Notice:                                                         |
//|                                                                            |
//|      Copyright (C) 2024, Example Corporation                               |
//|      All rights reserved                                                   |
//+----------------------------------------------------------------------------+
        package com.example.webservice;

import java.util.HashMap;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.apache.log4j.Logger;

import com.example.log.ServiceLogger;
import com.example.util.ServiceParameters;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;


@WebService(endpointInterface = "com.example.webservice.ExampleService")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class ServiceImpl implements Service {

    // Service manager, supposed to be thread safe
    private final ExampleServiceManager manager = new ExampleServiceManager();
    private static Logger logger = Logger.getLogger(ServiceLogger.class.getName());

    @Override
    public String processRequest(@WebParam(name = "payload") String payload) {
        HashMap<String, Object> inputMap = new HashMap<>();
        HashMap<String, Object> outputMap = new HashMap<>();

        logger.debug(payload);

        if (payload == null) {
            logger.error("Received null payload");
            outputMap.put(ServiceParameters.RESPONSE_STATUS, "No payload received");
            return serialize(outputMap);
        }
<fim_suffix>


        String action = (String) inputMap.get(ServiceParameters.ACTION);
        if (action == null || action.isEmpty()) {
            outputMap.put(ServiceParameters.RESPONSE_STATUS, "No action specified");
            return serialize(outputMap);
        }

        // Begin to route actions
        if ("create".equals(action)) {
            outputMap = manager.createResource(inputMap);
        } else if ("update".equals(action)) {
            outputMap = manager.updateResource(inputMap);
        } else if ("delete".equals(action)) {
            outputMap = manager.deleteResource(inputMap);
        } else if ("query".equals(action)) {
            outputMap = manager.queryResource(inputMap);
        } else {
            outputMap.put(ServiceParameters.RESPONSE_STATUS, "Unknown action: " + action);
        }

        return serialize(outputMap);
    }

    private String serialize(HashMap<String, Object> map) {
        JSONSerializer serializer = new JSONSerializer();
        return serializer.deepSerialize(map);
    }

}
<fim_middle>