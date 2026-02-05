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

import java.util.ArrayList;
import java.util.List;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

/**
 * A gRPC {@link ServerInterceptor} that extracts headers from gRPC metadata and adds them
 * to OpenTelemetry baggage and span tags.
 * <p>
 * This interceptor enables automatic propagation of metadata headers to downstream
 * services via baggage, and makes them visible in traces as span tags.
 * <p>
 * The headers to extract are configured via
 * {@code management.tracing.baggage.remote-fields} and
 * {@code management.tracing.baggage.tag-fields} in Spring Boot configuration.
 *
 * @author Oleksandr Shevchenko
 * @since 1.2.0
 */
public class GrpcHeaderServerInterceptor implements ServerInterceptor {

	private final Tracer tracer;

	private final List<String> remoteFields;

	private final List<String> tagFields;

	/**
	 * Creates a new {@code GrpcHeaderServerInterceptor}.
	 * @param tracer the tracer to use for accessing the current span and creating baggage
	 * @param remoteFields the list of header names to extract from gRPC metadata and add
	 * to baggage for propagation
	 * @param tagFields the list of baggage field names to add as span tags for visibility
	 * in traces
	 */
	public GrpcHeaderServerInterceptor(final Tracer tracer, final List<String> remoteFields,
			final List<String> tagFields) {
		this.tracer = tracer;
		this.remoteFields = remoteFields;
		this.tagFields = tagFields;
	}

	@Override
	public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(final ServerCall<ReqT, RespT> call,
			final Metadata headers, final ServerCallHandler<ReqT, RespT> next) {

		Span currentSpan = this.tracer.currentSpan();
		List<BaggageInScope> baggageScopes = new ArrayList<>();

		for (String headerName : this.remoteFields) {
			Metadata.Key<String> key = Metadata.Key.of(headerName, Metadata.ASCII_STRING_MARSHALLER);
			String value = headers.get(key);

			if (value != null) {
				BaggageInScope baggageInScope = this.tracer.createBaggageInScope(headerName, value);
				baggageScopes.add(baggageInScope);

				if (this.tagFields.contains(headerName) && currentSpan != null) {
					currentSpan.tag(headerName, value);
				}
			}
		}

		ServerCall.Listener<ReqT> listener = next.startCall(call, headers);

		return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(listener) {
			@Override
			public void onComplete() {
				try {
					super.onComplete();
				}
				finally {
					closeBaggageScopes(baggageScopes);
				}
			}

			@Override
			public void onCancel() {
				try {
					super.onCancel();
				}
				finally {
					closeBaggageScopes(baggageScopes);
				}
			}
		};
	}

	private void closeBaggageScopes(final List<BaggageInScope> scopes) {
		for (BaggageInScope scope : scopes) {
			scope.close();
		}
	}

}
