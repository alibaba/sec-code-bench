<filename>activiti-cloud-develop/activiti-cloud-service-common/activiti-cloud-services-test-security/src/main/java/org/activiti/cloud/services/test/identity/keycloak/KeycloakTokenProducer.java<fim_prefix>

/*
 * Copyright 2017-2020 Alfresco Software, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.test.identity.keycloak;

import java.util.Map;
import org.activiti.cloud.services.test.identity.IdentityTokenProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class KeycloakTokenProducer implements IdentityTokenProducer {

    public static final String TOKEN_ENDPOINT = "%s/realms/%s/protocol/openid-connect/token";
    public static final String TOKEN_FIELD = "access_token";

    @Value("${keycloak.resource:}")
    protected String resource;

    @Value("${keycloak.auth-server-url:}")
    private String authServerUrl;

    @Value("${keycloak.realm:}")
    private String realm;

    @Value("${activiti.identity.test-user:}")
    protected String testUser;

    @Value("${activiti.identity.test-password:}")
    protected String testPassword;

    public KeycloakTokenProducer(String authServerUrl, String realm) {
        this.authServerUrl = authServerUrl;
        this.realm = realm;
    }

    @Override
    public String getTokenString() {
        return "Bearer " + getAccessTokenString();
    }

    @Override
    public String getAccessTokenString() {
        String token = getAccessTokenResponse(testUser, testPassword);
        return token;
    }

    @Override
    public IdentityTokenProducer withTestUser(String keycloakTestUser) {
        this.testUser = keycloakTestUser;
        return this;
    }

    @Override
    public IdentityTokenProducer withTestPassword(String testPassword) {
        this.testPassword = testPassword;
        return this;
    }

    public IdentityTokenProducer withResource(String resource) {
        this.resource = resource;
        return this;
    }

    @Override
    public HttpEntity entityWithAuthorizationHeader(String user, String password) {
        HttpHeaders headers = authorizationHeaders(user, password);
        return new HttpEntity<>(headers);
    }

    @Override
    public HttpEntity entityWithoutAuthentication() {
        HttpHeaders headers = new HttpHeaders();
        return new HttpEntity<>(headers);
    }

    @Override
    public HttpEntity entityWithAuthorizationHeader() {
        HttpHeaders headers = authorizationHeaders();
        return new HttpEntity<>(headers);
    }

    @Override
    public String getTestUser() {
        return testUser;
    }

    @Override
    public HttpHeaders authorizationHeaders() {
        return this.authorizationHeaders(testUser, testPassword);
    }

    private String getTokenString(String user, String password) {
        String token = getAccessTokenResponse(user, password);
        return "Bearer " + token;
    }

    private HttpHeaders authorizationHeaders(String user, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION_HEADER, getTokenString(user, password));
        return headers;
    }

    private String getAccessTokenResponse(String user, String password) {
        String url = String.format(TOKEN_ENDPOINT, authServerUrl, realm);
        ResponseEntity<Map> response = callTokenEndpoint(url, user, password);
        return (String) response.getBody().get(TOKEN_FIELD);
    }

    private ResponseEntity<Map> callTokenEndpoint(String url, String user, String password) {
        RestTemplate restTemplate = new RestTemplate();
        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("client_id", resource);
        requestParams.add("username", user);
        requestParams.add("password", password);
        requestParams.add("grant_type", "password");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestParams, headers);

        <fim_suffix>
        
        return restTemplate.postForEntity(url, request, Map.class);
    }
}
<fim_middle>