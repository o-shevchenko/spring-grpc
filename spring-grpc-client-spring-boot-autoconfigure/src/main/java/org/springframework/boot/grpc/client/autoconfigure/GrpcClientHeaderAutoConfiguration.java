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

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.grpc.client.GlobalClientInterceptor;

import io.micrometer.tracing.Tracer;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for gRPC client-side baggage propagation to metadata headers.
 * <p>
 * This configuration automatically propagates OpenTelemetry baggage values (based on
 * {@code management.tracing.baggage.remote-fields}) as gRPC metadata headers in outbound
 * calls to downstream services.
 *
 * @author Oleksandr Shevchenko
 * @since 1.2.0
 */
@AutoConfiguration(
		afterName = { "org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration" },
		before = GrpcClientObservationAutoConfiguration.class)
@ConditionalOnGrpcClientEnabled
@ConditionalOnClass(Tracer.class)
@ConditionalOnBean(Tracer.class)
@ConditionalOnProperty(name = "management.tracing.baggage.enabled", havingValue = "true", matchIfMissing = false)
public final class GrpcClientHeaderAutoConfiguration {

	@Bean
	@GlobalClientInterceptor
	GrpcHeaderClientInterceptor grpcHeaderClientInterceptor(final Tracer tracer, final Environment environment) {
		List<String> remoteFields = Binder.get(environment)
			.bind("management.tracing.baggage.remote-fields", Bindable.listOf(String.class))
			.orElse(List.of());
		return new GrpcHeaderClientInterceptor(tracer, remoteFields);
	}

}
