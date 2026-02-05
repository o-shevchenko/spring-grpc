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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.micrometer.tracing.Baggage;
import io.micrometer.tracing.Tracer;

class GrpcHeaderClientInterceptorTests {

	private Tracer tracer;

	private Channel channel;

	private ClientCall<String, String> clientCall;

	@BeforeEach
	void setUp() {
		this.tracer = mock(Tracer.class);
		this.channel = mock(Channel.class);
		this.clientCall = mock(ClientCall.class);
	}

	@Test
	void shouldAddBaggageToMetadataHeaders() {
		List<String> remoteFields = List.of("x-request-id", "x-user-id");
		GrpcHeaderClientInterceptor interceptor = new GrpcHeaderClientInterceptor(this.tracer, remoteFields);

		Baggage requestIdBaggage = mock(Baggage.class);
		when(requestIdBaggage.get()).thenReturn("req-123");
		Baggage userIdBaggage = mock(Baggage.class);
		when(userIdBaggage.get()).thenReturn("user-456");

		when(this.tracer.getBaggage("x-request-id")).thenReturn(requestIdBaggage);
		when(this.tracer.getBaggage("x-user-id")).thenReturn(userIdBaggage);

		MethodDescriptor<String, String> methodDescriptor = mock(MethodDescriptor.class);
		CallOptions callOptions = CallOptions.DEFAULT;

		when(this.channel.newCall(any(), any())).thenAnswer(invocation -> this.clientCall);

		ClientCall<String, String> interceptedCall = interceptor.interceptCall(methodDescriptor, callOptions,
				this.channel);

		ArgumentCaptor<Metadata> metadataCaptor = ArgumentCaptor.forClass(Metadata.class);
		ClientCall.Listener<String> listener = mock(ClientCall.Listener.class);
		interceptedCall.start(listener, new Metadata());

		verify(this.clientCall).start(any(), metadataCaptor.capture());

		Metadata capturedMetadata = metadataCaptor.getValue();
		assertThat(capturedMetadata.get(Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER)))
			.isEqualTo("req-123");
		assertThat(capturedMetadata.get(Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER)))
			.isEqualTo("user-456");
	}

	@Test
	void shouldNotAddHeaderWhenBaggageIsNull() {
		List<String> remoteFields = List.of("x-request-id");
		GrpcHeaderClientInterceptor interceptor = new GrpcHeaderClientInterceptor(this.tracer, remoteFields);

		when(this.tracer.getBaggage("x-request-id")).thenReturn(null);

		MethodDescriptor<String, String> methodDescriptor = mock(MethodDescriptor.class);
		CallOptions callOptions = CallOptions.DEFAULT;

		when(this.channel.newCall(any(), any())).thenAnswer(invocation -> this.clientCall);

		ClientCall<String, String> interceptedCall = interceptor.interceptCall(methodDescriptor, callOptions,
				this.channel);

		ArgumentCaptor<Metadata> metadataCaptor = ArgumentCaptor.forClass(Metadata.class);
		ClientCall.Listener<String> listener = mock(ClientCall.Listener.class);
		interceptedCall.start(listener, new Metadata());

		verify(this.clientCall).start(any(), metadataCaptor.capture());

		Metadata capturedMetadata = metadataCaptor.getValue();
		assertThat(capturedMetadata.get(Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER))).isNull();
	}

	@Test
	void shouldNotAddHeaderWhenBaggageValueIsNull() {
		List<String> remoteFields = List.of("x-request-id");
		GrpcHeaderClientInterceptor interceptor = new GrpcHeaderClientInterceptor(this.tracer, remoteFields);

		Baggage requestIdBaggage = mock(Baggage.class);
		when(requestIdBaggage.get()).thenReturn(null);
		when(this.tracer.getBaggage("x-request-id")).thenReturn(requestIdBaggage);

		MethodDescriptor<String, String> methodDescriptor = mock(MethodDescriptor.class);
		CallOptions callOptions = CallOptions.DEFAULT;

		when(this.channel.newCall(any(), any())).thenAnswer(invocation -> this.clientCall);

		ClientCall<String, String> interceptedCall = interceptor.interceptCall(methodDescriptor, callOptions,
				this.channel);

		ArgumentCaptor<Metadata> metadataCaptor = ArgumentCaptor.forClass(Metadata.class);
		ClientCall.Listener<String> listener = mock(ClientCall.Listener.class);
		interceptedCall.start(listener, new Metadata());

		verify(this.clientCall).start(any(), metadataCaptor.capture());

		Metadata capturedMetadata = metadataCaptor.getValue();
		assertThat(capturedMetadata.get(Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER))).isNull();
	}

	@Test
	void shouldHandleEmptyRemoteFields() {
		List<String> remoteFields = List.of();
		GrpcHeaderClientInterceptor interceptor = new GrpcHeaderClientInterceptor(this.tracer, remoteFields);

		MethodDescriptor<String, String> methodDescriptor = mock(MethodDescriptor.class);
		CallOptions callOptions = CallOptions.DEFAULT;

		when(this.channel.newCall(any(), any())).thenAnswer(invocation -> this.clientCall);

		ClientCall<String, String> interceptedCall = interceptor.interceptCall(methodDescriptor, callOptions,
				this.channel);

		ArgumentCaptor<Metadata> metadataCaptor = ArgumentCaptor.forClass(Metadata.class);
		ClientCall.Listener<String> listener = mock(ClientCall.Listener.class);
		interceptedCall.start(listener, new Metadata());

		verify(this.clientCall).start(any(), metadataCaptor.capture());

		Metadata capturedMetadata = metadataCaptor.getValue();
		assertThat(capturedMetadata.keys()).isEmpty();
	}

}
