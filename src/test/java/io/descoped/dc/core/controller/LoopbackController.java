package io.descoped.dc.core.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.descoped.dc.api.http.Request;
import io.descoped.dc.api.util.JsonParser;
import io.descoped.dc.application.spi.Controller;
import io.undertow.io.Receiver;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.util.Set;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;

public class LoopbackController implements Controller {

    @Override
    public String contextPath() {
        return "/echo";
    }

    @Override
    public Set<Request.Method> allowedMethods() {
        return Set.of(Request.Method.GET);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        JsonParser jsonParser = JsonParser.createJsonParser();
        ObjectNode objectNode = jsonParser.createObjectNode();

        {
            ObjectNode childObjectNode = jsonParser.createObjectNode();
            exchange.getRequestHeaders().getHeaderNames().forEach(h -> {
                exchange.getRequestHeaders().eachValue(h).forEach(v -> {
                    childObjectNode.put(h.toString(), v);
                });
            });
            objectNode.set("request-headers", childObjectNode);
        }

        {
            ObjectNode childObjectNode = jsonParser.createObjectNode();
            childObjectNode.put("uri", exchange.getRequestURI());
            childObjectNode.put("method", exchange.getRequestMethod().toString());
            childObjectNode.put("statusCode", String.valueOf(exchange.getStatusCode()));
            childObjectNode.put("isSecure", Boolean.valueOf(exchange.isSecure()).toString());
            childObjectNode.put("sourceAddress", exchange.getSourceAddress().toString());
//            childObjectNode.put("destinationAddress", exchange.getDestinationAddress().toString());
            objectNode.set("request-info", childObjectNode);
        }

        {
            ObjectNode childObjectNode = jsonParser.createObjectNode();
            exchange.requestCookies().forEach(cookie -> {
                childObjectNode.put(cookie.getName(), cookie.getValue());
            });
            objectNode.set("cookies", childObjectNode);
        }

        {
            ObjectNode childObjectNode = jsonParser.createObjectNode();
            exchange.getPathParameters().entrySet().forEach((e) -> {
                childObjectNode.put(e.getKey(), e.getValue().element());
            });
            objectNode.set("path-parameters", childObjectNode);
        }

        {
            objectNode.put("queryString", exchange.getQueryString());
            ObjectNode childObjectNode = jsonParser.createObjectNode();
            exchange.getQueryParameters().entrySet().forEach((e) -> {
                childObjectNode.put(e.getKey(), e.getValue().element());
            });
            objectNode.set("query-parameters", childObjectNode);
        }

        {
            objectNode.put("contentLength", String.valueOf(exchange.getRequestContentLength()));
            ObjectNode childObjectNode = jsonParser.createObjectNode();
            exchange.getRequestReceiver().receiveFullBytes(new Receiver.FullBytesCallback() {
                @Override
                public void handle(HttpServerExchange httpServerExchange, byte[] bytes) {
                    childObjectNode.put("payload", new String(bytes));
                }
            });
            objectNode.set("request-body", childObjectNode);
        }

        {
            ObjectNode childObjectNode = jsonParser.createObjectNode();
            exchange.getResponseHeaders().getHeaderNames().forEach(h -> {
                exchange.getResponseHeaders().eachValue(h).forEach(v -> {
                    childObjectNode.put(h.toString(), v);
                });
            });
            objectNode.set("response-headers", childObjectNode);
        }

        {
            ObjectNode childObjectNode = jsonParser.createObjectNode();
            exchange.responseCookies().forEach(cookie -> {
                childObjectNode.put(cookie.getName(), cookie.getValue());
            });
            objectNode.set("response-cookies", childObjectNode);
        }

        if ("GET".equalsIgnoreCase(exchange.getRequestMethod().toString()))
            exchange.setStatusCode(HTTP_OK);
        else if ("POST".equalsIgnoreCase(exchange.getRequestMethod().toString()))
            exchange.setStatusCode(HTTP_NO_CONTENT);
        else
            throw new UnsupportedOperationException("Method " + exchange.getRequestMethod() + " not supported!");

        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        exchange.getResponseSender().send(jsonParser.toPrettyJSON(objectNode));
    }
}
