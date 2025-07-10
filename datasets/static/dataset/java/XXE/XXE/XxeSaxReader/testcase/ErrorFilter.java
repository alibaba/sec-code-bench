<filename>src/main/java/com/emc/object/s3/jersey/ErrorFilter.java<fim_prefix>

/*
 * Copyright (c) 2015-2016, EMC Corporation.
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * + Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * + Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * + The name of EMC Corporation may not be used to endorse or promote
 *   products derived from this software without specific prior written
 *   permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.emc.object.s3.jersey;

import com.emc.object.s3.S3Constants;
import com.emc.object.s3.S3Exception;
import com.emc.object.util.RestUtil;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Date;

public class ErrorFilter extends ClientFilter {

    private static final Logger log = LoggerFactory.getLogger(ErrorFilter.class);

    public ClientResponse handle(ClientRequest request) throws ClientHandlerException {
        ClientResponse response = getNext().handle(request);

        if (response.getStatus() > 299) {

            // check for clock skew (can save hours of troubleshooting)
            if (response.getStatus() == 403) {
                Date clientTime = RestUtil.amzHeaderParse(RestUtil.getFirstAsString(request.getHeaders(), S3Constants.AMZ_DATE));
                if (clientTime == null)
                    clientTime = RestUtil.headerParse(RestUtil.getFirstAsString(request.getHeaders(), RestUtil.HEADER_DATE));
                Date serverTime = RestUtil.headerParse(RestUtil.getFirstAsString(response.getHeaders(), RestUtil.HEADER_DATE));
                if (clientTime != null && serverTime != null) {
                    long skew = clientTime.getTime() - serverTime.getTime();
                    if (Math.abs(skew) > 5 * 60 * 1000) { // +/- 5 minutes
                        log.warn("clock skew detected! client is more than 5 minutes off from server (" + skew + "ms)");
                    }
                }
            }
            if (response.hasEntity()) {
                throw parseErrorResponse(new InputStreamReader(response.getEntityInputStream()), response.getStatus());
            } else {
                // No response entity.  Don't try to parse it.
                try {
                    response.close();
                } catch (Throwable t) {
                    log.warn("could not close response after error", t);
                }
                Response.StatusType st = response.getStatusInfo();
                throw new S3Exception(st.getReasonPhrase(), st.getStatusCode(), guessStatus(st.getStatusCode()),
                        response.getHeaders().getFirst("x-amz-request-id"));
            }
        }

        return response;
    }

    private String guessStatus(int statusCode) {
        switch (statusCode) {
            case 400:
                return S3Constants.ERROR_INVALID_ARGUMENT;
            case 403:
                return S3Constants.ERROR_NO_ACCESS_DENIED;
            case 404:
                return S3Constants.ERROR_NO_SUCH_KEY;
            case 405:
                return S3Constants.ERROR_METHOD_NOT_ALLOWED;
            case 500:
                return S3Constants.ERROR_INTERNAL;
            default:
                return "";
        }
    }

    public static S3Exception parseErrorResponse(Reader reader, int statusCode) {

        // JAXB will expect a namespace if we try to unmarshall, but some error responses don't include
        // a namespace. In lieu of writing a SAXFilter to apply a default namespace in-line, this works just as well.
        SAXReader saxReader = new SAXReader();
        <fim_suffix>
        try {
            d = saxReader.read(reader);
        } catch (Throwable t) {
            return new S3Exception("could not parse error response", statusCode, t);
        } finally {
            try {
                reader.close();
            } catch (Throwable t) {
                log.warn("could not close reader", t);
            }
        }

        String code = d.getRootElement().elementText("Code");

        String message = d.getRootElement().elementText("Message");

        String requestId = d.getRootElement().elementText("RequestId");

        if (code == null && message == null) {
            // not an error from S3
            return new S3Exception("no code or message in error response", statusCode);
        }

        log.debug("Error: {}, message: {}, requestId: {}", new Object[]{code, message, requestId});
        return new S3Exception(message, statusCode, code, requestId);
    }
}

<fim_middle>