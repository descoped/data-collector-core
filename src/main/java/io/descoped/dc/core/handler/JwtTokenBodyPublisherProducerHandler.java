package io.descoped.dc.core.handler;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.el.ExpressionLanguage;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.node.BodyPublisherProducer;
import io.descoped.dc.api.node.JwtIdentity;
import io.descoped.dc.api.node.JwtTokenBodyPublisherProducer;
import io.descoped.dc.core.security.CertificateContext;
import io.descoped.dc.core.security.CertificateFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Handler(forClass = JwtTokenBodyPublisherProducer.class)
public class JwtTokenBodyPublisherProducerHandler extends AbstractHandler<JwtTokenBodyPublisherProducer> {

    private static final Logger LOG = LoggerFactory.getLogger(JwtTokenBodyPublisherProducer.class);

    public JwtTokenBodyPublisherProducerHandler(JwtTokenBodyPublisherProducer node) {
        super(node);
    }

    @Override
    public ExecutionContext execute(ExecutionContext context) {
        // evaluate variables and replace map
        Map<String, Object> evaluatedVariablesMap = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : context.variables().entrySet()) {
            evaluatedVariablesMap.put(entry.getKey(), evaluateExpression(context, (String) entry.getValue()));
        }
        ExecutionContext jwtTokenContext = new ExecutionContext.Builder().of(context).variables(evaluatedVariablesMap).build();

        // create jwt grant
        JwtIdentity jwtIdentity = (JwtIdentity) node.identity();

        JWTCreator.Builder jwtBuilder = JWT.create();

        CertificateFactory certificateFactory = context.services().get(CertificateFactory.class);
        CertificateContext certificateContext = certificateFactory.getCertificateContext(jwtIdentity.headerClaims().sslBundleName());

        KeyPair keyPair = certificateContext.keyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        Algorithm algorithm = Algorithm.RSA256(publicKey, privateKey);

        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("alg", jwtIdentity.headerClaims().alg());
        headers.put("x5c", List.of(certificateContext.trustManager().getAcceptedIssuers()).stream().map(this::getEncodedCertificate).collect(Collectors.toList()));
        jwtBuilder.withHeader(headers);

        jwtBuilder.withIssuer(evaluateExpression(jwtTokenContext, jwtIdentity.claims().issuer()));
        jwtBuilder.withAudience(jwtIdentity.claims().audience());

        // custom claims
        for (Map.Entry<String, String> entry : jwtIdentity.claims().getClaims().entrySet()) {
            jwtBuilder.withClaim(entry.getKey(), entry.getValue());
        }

        String ttl = evaluateExpression(jwtTokenContext, jwtIdentity.claims().timeToLiveInSeconds());
        if (ttl == null || "null".equals(ttl)) {
            throw new IllegalStateException("Jwt time-to-live is NOT defined!");
        }
        long expirationInSeconds = Long.parseLong(ttl);
        OffsetDateTime now = Instant.now().atOffset(ZoneOffset.UTC);
        jwtBuilder.withIssuedAt(Date.from(now.toInstant()));
        jwtBuilder.withExpiresAt(Date.from(now.plusSeconds(expirationInSeconds).toInstant()));
        jwtBuilder.withJWTId(UUID.randomUUID().toString());

        String token = jwtBuilder.sign(algorithm);

        ExecutionContext evalContext = ExecutionContext.of(context);
        evalContext.variable(node.bindTo(), token);

        String jwtGrant = evaluateExpression(evalContext, node.token());
        //System.out.printf("jwtGrant => %s%n", jwtGrant);

        LOG.info("Produce signed JwtGrant");
        return ExecutionContext.empty().state(BodyPublisherProducer.class, jwtGrant.getBytes());
    }

    byte[] getEncodedCertificate(X509Certificate crt) {
        try {
            crt.checkValidity();
            return crt.getEncoded();
        } catch (CertificateEncodingException | CertificateExpiredException | CertificateNotYetValidException e) {
            throw new RuntimeException(e);
        }
    }

    private String evaluateExpression(ExecutionContext context, String text) {
        ExpressionLanguage el = new ExpressionLanguage(context);
        if (el.isExpression(text)) {
            return el.evaluateExpressions(text);
        } else {
            return text;
        }
    }

}
