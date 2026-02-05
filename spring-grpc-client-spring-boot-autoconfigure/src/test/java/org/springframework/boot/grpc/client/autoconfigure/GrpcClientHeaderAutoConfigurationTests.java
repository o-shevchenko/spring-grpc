/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.grpc.client.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.micrometer.tracing.Tracer;

class GrpcClientHeaderAutoConfigurationTests {

	private final ApplicationContextRunner baseContextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(GrpcClientHeaderAutoConfiguration.class));

	private ApplicationContextRunner validContextRunner() {
		return new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GrpcClientHeaderAutoConfiguration.class))
			.withBean("tracer", Tracer.class, () -> mock(Tracer.class))
			.withPropertyValues("management.tracing.baggage.enabled=true",
					"management.tracing.baggage.remote-fields=x-request-id,x-user-id");
	}

	@Test
	void whenGrpcNotOnClasspathAutoConfigurationIsSkipped() {
		this.validContextRunner()
			.withClassLoader(new FilteredClassLoader(io.grpc.stub.AbstractStub.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcClientHeaderAutoConfiguration.class));
	}

	@Test
	void whenTracerNotOnClasspathAutoConfigSkipped() {
		this.validContextRunner()
			.withClassLoader(new FilteredClassLoader(Tracer.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcClientHeaderAutoConfiguration.class));
	}

	@Test
	void whenTracerNotProvidedThenAutoConfigSkipped() {
		this.baseContextRunner.withPropertyValues("management.tracing.baggage.enabled=true")
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcClientHeaderAutoConfiguration.class));
	}

	@Test
	void whenBaggagePropertyDisabledThenAutoConfigIsSkipped() {
		this.validContextRunner()
			.withPropertyValues("management.tracing.baggage.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcClientHeaderAutoConfiguration.class));
	}

	@Test
	void whenBaggagePropertyNotSetThenAutoConfigIsSkipped() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(GrpcClientHeaderAutoConfiguration.class))
			.withBean("tracer", Tracer.class, () -> mock(Tracer.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcClientHeaderAutoConfiguration.class));
	}

	@Test
	void whenBaggagePropertyEnabledAndClientNotDisabledThenAutoConfigNotSkipped() {
		this.validContextRunner()
			.run((context) -> assertThat(context).hasSingleBean(GrpcClientHeaderAutoConfiguration.class));
	}

	@Test
	void whenBaggagePropertyEnabledThenInterceptorIsCreated() {
		this.validContextRunner()
			.run((context) -> assertThat(context).hasSingleBean(GrpcHeaderClientInterceptor.class));
	}

}
