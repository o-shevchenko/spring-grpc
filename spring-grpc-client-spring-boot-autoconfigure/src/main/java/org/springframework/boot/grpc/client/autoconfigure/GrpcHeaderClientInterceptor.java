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

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.micrometer.tracing.Baggage;
import io.micrometer.tracing.Tracer;

/**
 * A gRPC {@link ClientInterceptor} that propagates OpenTelemetry baggage to outbound gRPC
 * calls by adding them as metadata headers.
 * <p>
 * This interceptor ensures that baggage values are automatically forwarded to downstream
 * services in gRPC metadata headers.
 * <p>
 * The baggage fields to propagate are configured via
 * {@code management.tracing.baggage.remote-fields} in Spring Boot configuration.
 *
 * @author Oleksandr Shevchenko
 * @since 1.2.0
 */
public class GrpcHeaderClientInterceptor implements ClientInterceptor {

	private final Tracer tracer;

	private final List<String> remoteFields;

	/**
	 * Creates a new {@code GrpcHeaderClientInterceptor}.
	 * @param tracer the tracer to use for accessing baggage
	 * @param remoteFields the list of baggage field names to propagate as gRPC metadata
	 * headers
	 */
	public GrpcHeaderClientInterceptor(final Tracer tracer, final List<String> remoteFields) {
		this.tracer = tracer;
		this.remoteFields = remoteFields;
	}

	@Override
	public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(final MethodDescriptor<ReqT, RespT> method,
			final CallOptions callOptions, final Channel next) {

		return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
			@Override
			public void start(final Listener<RespT> responseListener, final Metadata headers) {
				for (String fieldName : GrpcHeaderClientInterceptor.this.remoteFields) {
					Baggage baggage = GrpcHeaderClientInterceptor.this.tracer.getBaggage(fieldName);
					if (baggage != null) {
						String value = baggage.get();
						if (value != null) {
							Metadata.Key<String> key = Metadata.Key.of(fieldName, Metadata.ASCII_STRING_MARSHALLER);
							headers.put(key, value);
						}
					}
				}
				super.start(responseListener, headers);
			}
		};
	}

}
