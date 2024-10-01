package io.descoped.dc.core.handler;

import com.github.mizosoft.methanol.MultipartBodyPublisher;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.http.Request;
import io.descoped.dc.api.node.BodyPart;
import io.descoped.dc.api.node.BodyPublisher;
import io.descoped.dc.api.node.BodyPublisherProducer;
import io.descoped.dc.core.executor.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.Flow;

@Handler(forClass = BodyPublisher.class)
public class BodyPublisherHandler extends AbstractHandler<BodyPublisher> {

    private static final Logger LOG = LoggerFactory.getLogger(BodyPublisherHandler.class);

    public BodyPublisherHandler(BodyPublisher node) {
        super(node);
    }

    @Override
    public ExecutionContext execute(ExecutionContext context) {
        Request.Builder requestBuilder = context.state(Request.Builder.class);
        if (requestBuilder == null) {
            LOG.error("Unable to resolve Request.Builder!");
            return ExecutionContext.empty();
        }

        Flow.Publisher<ByteBuffer> byteArrayBodyPublisher;

        switch (node.getEncoding()) {
            case TEXT_PLAIN:
                if (node.getPlainText() == null) {
                    byteArrayBodyPublisher = HttpRequest.BodyPublishers.ofByteArray(new byte[0]);
                } else {
                    ExecutionContext bodyPublisherOutput = Executor.execute(node.getPlainText(), context);
                    byte[] bytesPlainText = bodyPublisherOutput.state((Object) BodyPublisherProducer.class);
                    byteArrayBodyPublisher = HttpRequest.BodyPublishers.ofByteArray(bytesPlainText);
                }
                break;

            case APPLICATION_X_WWW_FORM_URLENCODED:
                requestBuilder.header("Content-Type", node.getEncoding().getMimeType());
                if (node.getUrlEncodedData() == null) {
                    byteArrayBodyPublisher = HttpRequest.BodyPublishers.ofByteArray(new byte[0]);
                } else {
                    ExecutionContext bodyPublisherOutput = Executor.execute(node.getUrlEncodedData(), context);
                    byte[] bytesUrlEncodedData = bodyPublisherOutput.state((Object) BodyPublisherProducer.class);
                    byteArrayBodyPublisher = HttpRequest.BodyPublishers.ofByteArray(bytesUrlEncodedData);
                }
                break;

            case MULTIPART_FORM_DATA:
                MultipartBodyPublisher.Builder builder = MultipartBodyPublisher.newBuilder();
                for (BodyPart part : node.getParts()) {
                    if (part.isTextPart()) {
                        builder.textPart(part.name, part.value);

                    } else if (part.isFormPart()) {
                        Objects.requireNonNull(part.filename);

                        if (part.value instanceof byte[]) {
                            byte[] partBytes = (byte[]) part.value;
                            builder.formPart(part.name, part.filename, HttpRequest.BodyPublishers.ofByteArray(partBytes));

                        } else if (part.value instanceof String) {
                            String partString = (String) part.value;
                            builder.formPart(part.name, part.filename, HttpRequest.BodyPublishers.ofByteArray(partString.getBytes()));

                        } else {
                            throw new UnsupportedOperationException();
                        }
                    }
                }
                MultipartBodyPublisher multipartBodyPublisher = builder.build();

                requestBuilder.header("Content-Type", node.getEncoding().getMimeType() + ";boundary=" + multipartBodyPublisher.boundary());
                byteArrayBodyPublisher = multipartBodyPublisher;
                break;

            default:
                throw new UnsupportedOperationException();
        }

        return ExecutionContext.empty().state(BodyPublisher.BODY_PUBLISHER_RESULT, byteArrayBodyPublisher);
    }

}
