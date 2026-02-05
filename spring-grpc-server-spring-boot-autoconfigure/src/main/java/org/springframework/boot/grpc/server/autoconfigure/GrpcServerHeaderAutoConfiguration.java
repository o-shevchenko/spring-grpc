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

package org.springframework.boot.grpc.server.autoconfigure;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.grpc.server.GlobalServerInterceptor;

import io.micrometer.tracing.Tracer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for gRPC server-side baggage
 * propagation from metadata headers.
 * <p>
 * This configuration automatically extracts headers from gRPC metadata (based on
 * {@code management.tracing.baggage.remote-fields}) and:
 * <ul>
 * <li>Creates OpenTelemetry baggage for propagation to downstream services</li>
 * <li>Adds them as span tags (based on {@code management.tracing.baggage.tag-fields}) for
 * visibility in traces</li>
 * </ul>
 *
 * @author Oleksandr Shevchenko
 * @since 1.2.0
 */
@AutoConfiguration(
		afterName = { "org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration" },
		before = GrpcServerObservationAutoConfiguration.class)
@ConditionalOnSpringGrpc
@ConditionalOnClass(Tracer.class)
@ConditionalOnGrpcServerEnabled("baggage")
@ConditionalOnBean(Tracer.class)
@ConditionalOnProperty(name = "management.tracing.baggage.enabled", havingValue = "true", matchIfMissing = false)
public final class GrpcServerHeaderAutoConfiguration {

	@Bean
	@Order(-10)
	@GlobalServerInterceptor
	GrpcHeaderServerInterceptor grpcHeaderServerInterceptor(final Tracer tracer, final Environment environment) {
		Binder binder = Binder.get(environment);
		List<String> remoteFields = binder
			.bind("management.tracing.baggage.remote-fields", Bindable.listOf(String.class))
			.orElse(List.of());
		List<String> tagFields = binder.bind("management.tracing.baggage.tag-fields", Bindable.listOf(String.class))
			.orElse(List.of());
		return new GrpcHeaderServerInterceptor(tracer, remoteFields, tagFields);
	}

}
