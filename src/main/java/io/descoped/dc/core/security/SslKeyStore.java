package io.descoped.dc.core.security;

interface SslKeyStore {

    CertificateContext buildSSLContext();

}
