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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

class GrpcHeaderServerInterceptorTests {

	private Tracer tracer;

	private ServerCall<String, String> serverCall;

	private ServerCallHandler<String, String> serverCallHandler;

	private Metadata metadata;

	@BeforeEach
	void setUp() {
		this.tracer = mock(Tracer.class);
		this.serverCall = mock(ServerCall.class);
		this.serverCallHandler = mock(ServerCallHandler.class);
		this.metadata = new Metadata();
	}

	@Test
	void shouldExtractHeadersAndCreateBaggage() {
		List<String> remoteFields = List.of("x-request-id", "x-user-id");
		List<String> tagFields = List.of();
		GrpcHeaderServerInterceptor interceptor = new GrpcHeaderServerInterceptor(this.tracer, remoteFields, tagFields);

		this.metadata.put(Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER), "req-123");
		this.metadata.put(Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER), "user-456");

		BaggageInScope baggageInScope1 = mock(BaggageInScope.class);
		BaggageInScope baggageInScope2 = mock(BaggageInScope.class);
		when(this.tracer.createBaggageInScope("x-request-id", "req-123")).thenReturn(baggageInScope1);
		when(this.tracer.createBaggageInScope("x-user-id", "user-456")).thenReturn(baggageInScope2);

		ServerCall.Listener<String> mockListener = mock(ServerCall.Listener.class);
		when(this.serverCallHandler.startCall(this.serverCall, this.metadata)).thenReturn(mockListener);

		ServerCall.Listener<String> listener = interceptor.interceptCall(this.serverCall, this.metadata,
				this.serverCallHandler);

		verify(this.tracer).createBaggageInScope("x-request-id", "req-123");
		verify(this.tracer).createBaggageInScope("x-user-id", "user-456");
		assertThat(listener).isNotNull();
	}

	@Test
	void shouldAddSpanTagsWhenConfigured() {

		List<String> remoteFields = List.of("x-request-id");
		List<String> tagFields = List.of("x-request-id");
		GrpcHeaderServerInterceptor interceptor = new GrpcHeaderServerInterceptor(this.tracer, remoteFields, tagFields);

		this.metadata.put(Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER), "req-123");

		Span currentSpan = mock(Span.class);
		when(this.tracer.currentSpan()).thenReturn(currentSpan);

		BaggageInScope baggageInScope = mock(BaggageInScope.class);
		when(this.tracer.createBaggageInScope("x-request-id", "req-123")).thenReturn(baggageInScope);

		ServerCall.Listener<String> mockListener = mock(ServerCall.Listener.class);
		when(this.serverCallHandler.startCall(this.serverCall, this.metadata)).thenReturn(mockListener);

		interceptor.interceptCall(this.serverCall, this.metadata, this.serverCallHandler);

		verify(currentSpan).tag("x-request-id", "req-123");
	}

	@Test
	void shouldNotAddSpanTagsWhenNotConfigured() {

		List<String> remoteFields = List.of("x-request-id");
		List<String> tagFields = List.of();
		GrpcHeaderServerInterceptor interceptor = new GrpcHeaderServerInterceptor(this.tracer, remoteFields, tagFields);

		this.metadata.put(Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER), "req-123");

		Span currentSpan = mock(Span.class);
		when(this.tracer.currentSpan()).thenReturn(currentSpan);

		BaggageInScope baggageInScope = mock(BaggageInScope.class);
		when(this.tracer.createBaggageInScope("x-request-id", "req-123")).thenReturn(baggageInScope);

		ServerCall.Listener<String> mockListener = mock(ServerCall.Listener.class);
		when(this.serverCallHandler.startCall(this.serverCall, this.metadata)).thenReturn(mockListener);

		interceptor.interceptCall(this.serverCall, this.metadata, this.serverCallHandler);

		verify(currentSpan, never()).tag(eq("x-request-id"), eq("req-123"));
	}

	@Test
	void shouldCloseBaggageOnComplete() {
		List<String> remoteFields = List.of("x-request-id");
		List<String> tagFields = List.of();
		GrpcHeaderServerInterceptor interceptor = new GrpcHeaderServerInterceptor(this.tracer, remoteFields, tagFields);

		this.metadata.put(Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER), "req-123");

		BaggageInScope baggageInScope = mock(BaggageInScope.class);
		when(this.tracer.createBaggageInScope("x-request-id", "req-123")).thenReturn(baggageInScope);

		ServerCall.Listener<String> mockListener = mock(ServerCall.Listener.class);
		when(this.serverCallHandler.startCall(this.serverCall, this.metadata)).thenReturn(mockListener);

		ServerCall.Listener<String> listener = interceptor.interceptCall(this.serverCall, this.metadata,
				this.serverCallHandler);

		listener.onComplete();
		verify(baggageInScope).close();
	}

	@Test
	void shouldCloseBaggageOnCancel() {
		List<String> remoteFields = List.of("x-request-id");
		List<String> tagFields = List.of();
		GrpcHeaderServerInterceptor interceptor = new GrpcHeaderServerInterceptor(this.tracer, remoteFields, tagFields);

		this.metadata.put(Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER), "req-123");

		BaggageInScope baggageInScope = mock(BaggageInScope.class);
		when(this.tracer.createBaggageInScope("x-request-id", "req-123")).thenReturn(baggageInScope);

		ServerCall.Listener<String> mockListener = mock(ServerCall.Listener.class);
		when(this.serverCallHandler.startCall(this.serverCall, this.metadata)).thenReturn(mockListener);

		ServerCall.Listener<String> listener = interceptor.interceptCall(this.serverCall, this.metadata,
				this.serverCallHandler);

		listener.onCancel();
		verify(baggageInScope).close();
	}

	@Test
	void shouldNotCreateBaggageWhenHeaderNotPresent() {

		List<String> remoteFields = List.of("x-request-id");
		List<String> tagFields = List.of();
		GrpcHeaderServerInterceptor interceptor = new GrpcHeaderServerInterceptor(this.tracer, remoteFields, tagFields);

		ServerCall.Listener<String> mockListener = mock(ServerCall.Listener.class);
		when(this.serverCallHandler.startCall(this.serverCall, this.metadata)).thenReturn(mockListener);

		interceptor.interceptCall(this.serverCall, this.metadata, this.serverCallHandler);

		verify(this.tracer, never()).createBaggageInScope(eq("x-request-id"), eq("req-123"));
	}

	@Test
	void shouldHandleEmptyRemoteFields() {

		List<String> remoteFields = List.of();
		List<String> tagFields = List.of();
		GrpcHeaderServerInterceptor interceptor = new GrpcHeaderServerInterceptor(this.tracer, remoteFields, tagFields);

		this.metadata.put(Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER), "req-123");

		ServerCall.Listener<String> mockListener = mock(ServerCall.Listener.class);
		when(this.serverCallHandler.startCall(this.serverCall, this.metadata)).thenReturn(mockListener);

		interceptor.interceptCall(this.serverCall, this.metadata, this.serverCallHandler);

		verify(this.tracer, never()).createBaggageInScope(eq("x-request-id"), eq("req-123"));
	}

}
