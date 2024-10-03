import io.descoped.dc.api.http.Client;
import io.descoped.dc.api.http.Request;
import io.descoped.dc.api.http.Response;

module io.descoped.dc.core {
    requires io.descoped.dynamic.config;
    requires io.descoped.service.provider.api;
    requires io.descoped.rawdata.api;
    requires io.descoped.dc.api;
    requires io.descoped.secrets.client.api;
    requires io.descoped.secrets.provider.safe.configuration;
    requires io.descoped.secrets.provider.dynamic.configuration;

    requires java.logging;
    requires java.instrument;
    requires java.net.http;

    requires methanol;
    requires kotlin.stdlib;
    requires okhttp3;

    requires org.slf4j;
    requires jul.to.slf4j;

    requires net.bytebuddy;
    requires net.bytebuddy.agent;

    requires org.reactivestreams;
    requires io.reactivex.rxjava3;
    requires de.huxhorn.sulky.ulid;
    requires commons.jexl3;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires java.xml;
    requires hystrix.core;
    requires io.github.classgraph;
    requires org.bouncycastle.pkix;
    requires org.bouncycastle.provider;
    requires jackson.jq;

    requires com.auth0.jwt;

    requires simpleclient.common;
    requires simpleclient;
    requires simpleclient.hotspot;

    provides Client.Builder with io.descoped.dc.core.http.HttpClientDelegate.ClientBuilder;
    provides Request.Builder with io.descoped.dc.core.http.HttpRequestDelegate.RequestBuilder;
    provides Response.Builder with io.descoped.dc.core.http.HttpResponseDelegate.ResponseBuilder;

    opens io.descoped.dc.core;
    opens io.descoped.dc.core.server;
    opens io.descoped.dc.core.handler;

    exports io.descoped.dc.core.content;
    exports io.descoped.dc.core.executor;
    exports io.descoped.dc.core.handler;
    exports io.descoped.dc.core.http;
    exports io.descoped.dc.core.health;
    exports io.descoped.dc.core.metrics;
    exports io.descoped.dc.core.security;
    exports io.descoped.dc.core.util;

    // TODO API requires access to Core test scope. Added CircumventIllegalModulePackage to allow package exports and opens.
    exports io.descoped.dc.core.service;
    exports io.descoped.dc.core.controller;
}
