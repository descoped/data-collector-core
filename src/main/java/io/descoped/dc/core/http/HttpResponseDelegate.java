package io.descoped.dc.core.http;

import io.descoped.dc.api.http.BodyHandler;
import io.descoped.dc.api.http.Headers;
import io.descoped.dc.api.http.HttpStatus;
import io.descoped.dc.api.http.Response;

import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Optional;

import static java.util.Optional.ofNullable;

public class HttpResponseDelegate implements Response {

    final String url;
    final Headers headers;
    final int statusCode;
    final byte[] payload;
    final BodyHandler<?> bodyHandler;
    final Response previousResponse;

    private HttpResponseDelegate(String url,
                                 Headers headers,
                                 int statusCode,
                                 byte[] payload,
                                 BodyHandler<?> bodyHandler,
                                 Response previousResponse) {
        this.url = url;
        this.headers = headers;
        this.statusCode = statusCode;
        this.payload = payload;
        this.bodyHandler = bodyHandler;
        this.previousResponse = previousResponse;
    }

    @Override
    public String url() {
        return url;
    }

    @Override
    public Headers headers() {
        return headers;
    }

    @Override
    public int statusCode() {
        return statusCode;
    }

    @Override
    public byte[] body() {
        return payload;
    }

    @Override
    public <R> Optional<BodyHandler<R>> bodyHandler() {
        BodyHandler<R> handler = (BodyHandler<R>) bodyHandler;
        return Optional.ofNullable(handler);
    }

    @Override
    public Optional<Response> previousResponse() {
        return ofNullable(previousResponse);
    }

    @SuppressWarnings("unchecked")
    public static class ResponseBuilder implements Builder {

        HttpResponse<byte[]> httpResponse;
        BodyHandler<?> bodyHandler;

        @Override
        public Builder delegate(Object delegate) {
            this.httpResponse = (HttpResponse<byte[]>) delegate;
            return this;
        }

        @Override
        public <R> void bodyHandler(BodyHandler<R> bodyHandler) {
            this.bodyHandler = bodyHandler;
        }

        private Response previousResponse() {
            if (httpResponse != null && httpResponse.previousResponse().isPresent()) {
                HttpResponse<byte[]> response = httpResponse.previousResponse().orElseThrow();
                return Response.newResponseBuilder().delegate(response).build();
            }
            return null;
        }

        @Override
        public Response build() {
            return httpResponse == null ?
                    new HttpResponseDelegate(
                            "",
                            new Headers(new LinkedHashMap<>()),
                            HttpStatus.HTTP_NOT_ACCEPTABLE.code(),
                            new byte[0],
                            bodyHandler,
                            previousResponse()
                    ) :
                    new HttpResponseDelegate(
                            httpResponse.uri().toString(),
                            new Headers(httpResponse.headers().map()),
                            httpResponse.statusCode(),
                            httpResponse.body() == null ? new byte[0] : Arrays.copyOf(httpResponse.body(), httpResponse.body().length),
                            bodyHandler,
                            previousResponse()
                    );
        }
    }

}
