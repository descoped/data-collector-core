package io.descoped.dc.core.http;

import io.descoped.dc.api.http.Headers;
import io.descoped.dc.api.http.Request;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Flow;

public class OkHttpRequestDelegate implements Request {

    private final okhttp3.Request httpRequest;

    public OkHttpRequestDelegate(okhttp3.Request httpRequest) {
        this.httpRequest = httpRequest;
    }

    @Override
    public String url() {
        return httpRequest.url().toString();
    }

    @Override
    public Method method() {
        return Method.valueOf(httpRequest.method().toUpperCase());
    }

    @Override
    public Headers headers() {
        return new Headers(httpRequest.headers().toMultimap());
    }

    @Override
    public Object getDelegate() {
        return httpRequest;
    }

    public static class RequestBuilder implements Request.Builder {

        private static final Logger LOG = LoggerFactory.getLogger(RequestBuilder.class);

        String url;
        Request.Method method;
        Headers headers = new Headers();
        boolean enableExpectContinue;
        Duration timeoutDuration;
        byte[] payloadBytes;

        @Override
        public Request.Builder url(String url) {
            this.url = url;
            return this;
        }

        @Override
        public Request.Builder PUT(byte[] bytes) {
            this.method = Method.PUT;
            return this;
        }

        @Override
        public Builder PUT(Flow.Publisher<ByteBuffer> bodyPublisher) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Request.Builder POST(byte[] bytes) {
            payloadBytes = bytes;
            this.method = Method.POST;
            return this;
        }

        @Override
        public Builder POST(Flow.Publisher<ByteBuffer> bodyPublisher) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Request.Builder GET() {
            this.method = Method.GET;
            return this;
        }

        @Override
        public Request.Builder DELETE() {
            this.method = Method.DELETE;
            return this;
        }

        @Override
        public Request.Builder header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        @Override
        public Builder expectContinue(boolean enable) {
            enableExpectContinue = enable;
            return this;
        }

        @Override
        public Builder timeout(Duration duration) {
            this.timeoutDuration = duration;
            return this;
        }

        private void validate(Object... objects) {
            if (!Arrays.stream(objects).allMatch(Objects::nonNull)) {
                throw new RuntimeException("Null value");
            }
        }

        @Override
        public Request build() {
            validate(url, method);

            okhttp3.Request.Builder httpRequestBuilder = new okhttp3.Request.Builder();

            httpRequestBuilder.url(url);

            switch (method) {
                case PUT:
                    httpRequestBuilder.method(Method.PUT.name(), RequestBody.create(MediaType.parse(headers.firstValue("Content-Type").orElseThrow()), payloadBytes));
                    break;

                case POST:
                    httpRequestBuilder.method(Method.POST.name(), RequestBody.create(MediaType.parse(headers.firstValue("Content-Type").orElseThrow()), payloadBytes));
                    break;

                case GET:
                    httpRequestBuilder.method(Method.GET.name(), null);
                    break;

                case DELETE:
                    httpRequestBuilder.method(Method.DELETE.name(), null);
                    break;
            }

            for (Map.Entry<String, List<String>> entry : headers.asMap().entrySet()) {
                entry.getValue().forEach(value -> httpRequestBuilder.header(entry.getKey(), value));
            }

            // enableExpectContinue not supported
            //LOG.warn("Http Request enableExpectContinue NOT set, because it isn't supported by OkHttp");

            // timeoutDuration not supported
            //LOG.warn("Http Request timeoutDuration NOT set, because it isn't supported by OkHttp");

            return new OkHttpRequestDelegate(httpRequestBuilder.build());
        }
    }
}
