/*
 * Copyright 2013-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.kubernetes;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.test.LocalManagementPort;
import org.springframework.cloud.kubernetes.example.App;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = App.class,
		properties = { "management.health.kubernetes.enabled=false" })
@EnableKubernetesMockClient(crud = true, https = false)
class Fabric8HealthIndicatorDisabledTest {

	private static KubernetesClient mockClient;

	@Autowired
	private WebTestClient webClient;

	@LocalManagementPort
	private int port;

	@BeforeAll
	static void setUpBeforeClass() {
		// Configure the kubernetes master url to point to the mock server
		System.setProperty(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY, mockClient.getConfiguration().getMasterUrl());
		System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
		System.setProperty(Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY, "false");
		System.setProperty(Config.KUBERNETES_AUTH_TRYSERVICEACCOUNT_SYSTEM_PROPERTY, "false");
		System.setProperty(Config.KUBERNETES_NAMESPACE_SYSTEM_PROPERTY, "test");
	}

	@Test
	void healthEndpointShouldContainKubernetes() {
		this.webClient.get()
			.uri("http://localhost:{port}/actuator/health", this.port)
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("components.kubernetes")
			.doesNotExist();
	}

}
