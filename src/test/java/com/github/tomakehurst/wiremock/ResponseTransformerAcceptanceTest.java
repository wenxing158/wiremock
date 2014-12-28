/*
 * Copyright (C) 2011 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tomakehurst.wiremock;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.testsupport.WireMockResponse;
import com.github.tomakehurst.wiremock.testsupport.WireMockTestClient;
import org.junit.After;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ResponseTransformerAcceptanceTest {

    WireMockServer wm;
    WireMockTestClient client;

    @After
    public void cleanup() {
        wm.stop();
    }

    @Test
    public void transformerSpecifiedByClassTransformsHeadersStatusAndBody() {
        startWithExtensions("com.github.tomakehurst.wiremock.ResponseTransformerAcceptanceTest$ExampleTransformer");
        createStub("/to-transform");

        WireMockResponse response = client.get("/to-transform");
        assertThat(response.statusCode(), is(200));
        assertThat(response.firstHeader("MyHeader"), is("Transformed"));
        assertThat(response.content(), is("Transformed body"));
    }

    @Test
    public void supportsMultipleTransformers() {
        startWithExtensions(
                "com.github.tomakehurst.wiremock.ResponseTransformerAcceptanceTest$MultiTransformer1",
                "com.github.tomakehurst.wiremock.ResponseTransformerAcceptanceTest$MultiTransformer2");
        createStub("/to-multi-transform");

        WireMockResponse response = client.get("/to-multi-transform");
        assertThat(response.statusCode(), is(201));
        assertThat(response.content(), is("Expect this"));
    }

    @Test
    public void supportsSpecifiyingExtensionsByClass() {
        wm = new WireMockServer(wireMockConfig()
                .port(0)
                .extensions(ExampleTransformer.class, MultiTransformer1.class));
        wm.start();
        client = new WireMockTestClient(wm.port());
        createStub("/to-class-transform");

        WireMockResponse response = client.get("/to-class-transform");
        assertThat(response.statusCode(), is(201));
        assertThat(response.content(), is("Transformed body"));
    }

    @Test
    public void supportsSpecifiyingExtensionsByInstance() {
        wm = new WireMockServer(wireMockConfig()
                .port(0)
                .extensions(new ExampleTransformer(), new MultiTransformer2()));
        wm.start();
        client = new WireMockTestClient(wm.port());
        createStub("/to-instance-transform");

        WireMockResponse response = client.get("/to-instance-transform");
        assertThat(response.statusCode(), is(200));
        assertThat(response.content(), is("Expect this"));
    }

    private void startWithExtensions(String... extensions) {
        wm = new WireMockServer(wireMockConfig()
                .port(0)
                .extensions(extensions));
        wm.start();
        client = new WireMockTestClient(wm.port());
    }

    private void createStub(String url) {
        wm.stubFor(get(urlEqualTo(url)).willReturn(aResponse()
                .withHeader("MyHeader", "Initial")
                .withStatus(300)
                .withBody("Should not see this")));
    }

    public static class ExampleTransformer implements ResponseTransformer {

        @Override
        public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition) {
            return new ResponseDefinitionBuilder()
                    .withHeader("MyHeader", "Transformed")
                    .withStatus(200)
                    .withBody("Transformed body")
                    .build();
        }

        @Override
        public String name() {
            return "example";
        }
    }

    public static class MultiTransformer1 implements ResponseTransformer {

        @Override
        public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition) {
            return ResponseDefinitionBuilder
                    .like(responseDefinition).but()
                    .withStatus(201)
                    .build();
        }

        @Override
        public String name() {
            return "multi1";
        }
    }

    public static class MultiTransformer2 implements ResponseTransformer {

        @Override
        public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition) {
            return ResponseDefinitionBuilder
                    .like(responseDefinition).but()
                    .withBody("Expect this")
                    .build();
        }

        @Override
        public String name() {
            return "multi2";
        }
    }
}