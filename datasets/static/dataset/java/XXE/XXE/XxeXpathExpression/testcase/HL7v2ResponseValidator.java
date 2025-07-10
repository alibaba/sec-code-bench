<filename>schie1.0/mirth_connect/server/src/com/mirth/connect/plugins/datatypes/hl7v2/HL7v2ResponseValidator.java<fim_prefix>

        /*
         * Copyright (c) Mirth Corporation. All rights reserved.
         *
         * http://www.mirthcorp.com
         *
         * The software in this package is published under the terms of the MPL license a copy of which has
         * been included with this distribution in the LICENSE.txt file.
         */

        package com.mirth.connect.plugins.datatypes.hl7v2;

import java.io.CharArrayReader;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.mirth.connect.donkey.model.message.ConnectorMessage;
import com.mirth.connect.donkey.model.message.Response;
import com.mirth.connect.donkey.model.message.Status;
import com.mirth.connect.donkey.server.message.ResponseValidator;
import com.mirth.connect.model.datatype.ResponseValidationProperties;
import com.mirth.connect.model.datatype.SerializationProperties;
import com.mirth.connect.plugins.datatypes.hl7v2.HL7v2ResponseValidationProperties.OriginalMessageControlId;
import com.mirth.connect.server.util.TemplateValueReplacer;
import com.mirth.connect.util.ErrorMessageBuilder;
import com.mirth.connect.util.StringUtil;

public class HL7v2ResponseValidator implements ResponseValidator {

    private HL7v2SerializationProperties serializationProperties;
    private HL7v2ResponseValidationProperties responseValidationProperties;
    private String serializationSegmentDelimiter;
    private TemplateValueReplacer replacer = new TemplateValueReplacer();
    private static int MESSAGE_CONTROL_ID_FIELD = 10;

    public HL7v2ResponseValidator(SerializationProperties serializationProperties, ResponseValidationProperties responseValidationProperties) {
        this.serializationProperties = (HL7v2SerializationProperties) serializationProperties;
        this.responseValidationProperties = (HL7v2ResponseValidationProperties) responseValidationProperties;
        serializationSegmentDelimiter = StringUtil.unescape(this.serializationProperties.getSegmentDelimiter());
    }

    @Override
    public Response validate(Response response, ConnectorMessage connectorMessage) {
        HL7v2ResponseValidationProperties responseValidationProperties = getReplacedResponseValidationProperties(connectorMessage);
        String[] successfulACKCodes = StringUtils.split(responseValidationProperties.getSuccessfulACKCode(), ',');
        String[] errorACKCodes = StringUtils.split(responseValidationProperties.getErrorACKCode(), ',');
        String[] rejectedACKCodes = StringUtils.split(responseValidationProperties.getRejectedACKCode(), ',');
        boolean validateMessageControlId = responseValidationProperties.isValidateMessageControlId();

        String responseData = response.getMessage();

        if (StringUtils.isNotBlank(responseData)) {
            try {
                if (responseData.trim().startsWith("<")) {
                    // XML response received
                   handleXmlResponse(response, responseData, connectorMessage, successfulACKCodes, errorACKCodes, rejectedACKCodes, validateMessageControlId);
                } else {
                    // ER7 response received
                    handleEr7Response(response, responseData, connectorMessage, successfulACKCodes, errorACKCodes, rejectedACKCodes, validateMessageControlId);
                }
            } catch (Exception e) {
                response.setStatus(Status.QUEUED);
                response.setStatusMessage("Error validating response: " + e.getMessage());
                response.setError(ErrorMessageBuilder.buildErrorMessage(this.getClass().getSimpleName(), response.getStatusMessage(), e));
            }
        } else {
            response.setStatus(Status.QUEUED);
            response.setStatusMessage("Empty or blank response received.");
            response.setError(response.getStatusMessage());
        }

        return response;
    }

    private void handleXmlResponse(Response response, String responseData, ConnectorMessage connectorMessage,
                                   String[] successfulACKCodes, String[] errorACKCodes, String[] rejectedACKCodes, boolean validateMessageControlId) throws Exception {
<fim_suffix>
        String ackCode = XPathFactory.newInstance().newXPath().compile("//MSA.1/text()").evaluate(doc).trim();

        boolean rejected = Arrays.asList(rejectedACKCodes).contains(ackCode);
        boolean error = rejected || Arrays.asList(errorACKCodes).contains(ackCode);

        if (error || rejected) {
            String msa3 = StringUtils.trim(XPathFactory.newInstance().newXPath().compile("//MSA.3/text()").evaluate(doc));
            String err1 = StringUtils.trim(XPathFactory.newInstance().newXPath().compile("//ERR.1/text()").evaluate(doc));
            handleNACK(response, rejected, msa3, err1);
        } else if (Arrays.asList(successfulACKCodes).contains(ackCode)) {
            if (validateMessageControlId) {
                String msa2 = StringUtils.trim(XPathFactory.newInstance().newXPath().compile("//MSA.2/text()").evaluate(doc));
                String originalControlID = getOriginalControlId(connectorMessage);

                if (!StringUtils.equals(msa2, originalControlID)) {
                    handleInvalidControlId(response, originalControlID, msa2);
                } else {
                    response.setStatus(Status.SENT);
                }
            } else {
                response.setStatus(Status.SENT);
            }
        }
    }

    private void handleEr7Response(Response response, String responseData, ConnectorMessage connectorMessage,
                                   String[] successfulACKCodes, String[] errorACKCodes, String[] rejectedACKCodes, boolean validateMessageControlId) {

        if (serializationProperties.isConvertLineBreaks()) {
            responseData = StringUtil.convertLineBreaks(responseData, serializationSegmentDelimiter);
        }

        int index = -1;
        boolean valid = true;

        // Attempt to find the MSA segment using the segment delimiters in the serialization properties
        if ((index = responseData.indexOf(serializationSegmentDelimiter + "MSA")) >= 0) {
            // MSA found; add the length of the segment delimiter, MSA, and field separator to get to the index of MSA.1
            index += serializationSegmentDelimiter.length() + 4;

            if (index < responseData.length()) {
                char fieldSeparator = responseData.charAt(index - 1);
                boolean rejected = startsWithAny(responseData, rejectedACKCodes, index);
                boolean error = rejected || startsWithAny(responseData, errorACKCodes, index);

                if (error || rejected) {
                    String msa3 = extractEr7Field(responseData, index, fieldSeparator, 2); // MSA.3
                    String err1 = extractEr7Err1(responseData, fieldSeparator);
                    handleNACK(response, rejected, msa3, err1);
                } else if (startsWithAny(responseData, successfulACKCodes, index)) {
                    if (validateMessageControlId) {
                        String msa2 = extractEr7Field(responseData, index, fieldSeparator, 1); // MSA.2
                        String originalControlID = getOriginalControlId(connectorMessage);

                        if (!StringUtils.equals(msa2, originalControlID)) {
                            handleInvalidControlId(response, originalControlID, msa2);
                        } else {
                            response.setStatus(Status.SENT);
                        }
                    } else {
                        response.setStatus(Status.SENT);
                    }
                } else {
                    valid = false;
                }
            } else {
                valid = false;
            }
        } else {
            valid = false;
        }
        // 你可以返回valid以做日志或抛警告，此处略去
    }

    private Document convertToXmlDocument(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private String extractEr7Field(String responseData, int msaIndex, char fieldSeparator, int fieldNumber) {
        int index = msaIndex;
        for (int i = 0; i < fieldNumber; i++) {
            index = responseData.indexOf(fieldSeparator, index);
            if (index < 0) return null;
            index++;
        }
        // Look for next fieldSeparator or segment delimiter
        int nextSep = StringUtils.indexOfAny(responseData.substring(index), fieldSeparator + serializationSegmentDelimiter);
        if (nextSep >= 0) {
            return StringUtils.substring(responseData, index, index + nextSep);
        } else {
            return StringUtils.substring(responseData, index);
        }
    }

    private String extractEr7Err1(String responseData, char fieldSeparator) {
        int index = responseData.indexOf(serializationSegmentDelimiter + "ERR");
        if (index >= 0) {
            index += serializationSegmentDelimiter.length() + 4;
            String tempSegment = StringUtils.substring(responseData, index);
            int nextSep = StringUtils.indexOfAny(tempSegment, fieldSeparator + serializationSegmentDelimiter);
            if (nextSep >= 0) {
                return StringUtils.substring(tempSegment, 0, nextSep);
            } else {
                return StringUtils.substring(tempSegment, 0);
            }
        }
        return null;
    }

    private boolean startsWithAny(String str, String[] prefixes, int toffset) {
        for (String prefix : prefixes) {
            if (str.startsWith(prefix, toffset)) {
                return true;
            }
        }
        return false;
    }

    private String getOriginalControlId(ConnectorMessage connectorMessage) throws Exception {
        String controlId = "";
        return controlId;
    }

    private HL7v2ResponseValidationProperties getReplacedResponseValidationProperties(ConnectorMessage connectorMessage) {
        HL7v2ResponseValidationProperties props = new HL7v2ResponseValidationProperties(responseValidationProperties);

        props.setSuccessfulACKCode(replacer.replaceValues(props.getSuccessfulACKCode(), connectorMessage));
        props.setErrorACKCode(replacer.replaceValues(props.getErrorACKCode(), connectorMessage));
        props.setRejectedACKCode(replacer.replaceValues(props.getRejectedACKCode(), connectorMessage));

        return props;
    }

    private void handleNACK(Response response, boolean rejected, String msa3, String err1) {
    }

    private void handleInvalidControlId(Response response, String originalControlID, String msa2) {

    }
}

<fim_middle>