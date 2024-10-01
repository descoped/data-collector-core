package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.http.Request;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.api.node.BodyPublisher;
import io.descoped.dc.api.node.Identity;
import io.descoped.dc.api.node.Post;
import io.descoped.dc.core.executor.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.Flow;

@SuppressWarnings("unchecked")
@Handler(forClass = Post.class)
public class PostHandler extends OperationHandler<Post> {

    private static final Logger LOG = LoggerFactory.getLogger(PostHandler.class);

    public PostHandler(Post node) {
        super(node);
    }

    @Override
    public ExecutionContext execute(ExecutionContext input) {
        super.execute(input);

        int requestTimeout = beforeRequest(input);

        Request.Builder requestBuilder = Request.newRequestBuilder();

        // resolve security identity by name
        Optional<Identity> securityIdentity = node.configurations().security().identities().stream().filter(identity -> "some-prop".equals(identity.id())).findFirst();
        if (securityIdentity.isPresent()) {
            input.state(Identity.class, securityIdentity.get());
        }

        Flow.Publisher<ByteBuffer> byteBufferPublisher;
        if (node.bodyPublisher() != null) {
            ExecutionContext context = ExecutionContext.of(input).state(Request.Builder.class, requestBuilder);
            ExecutionContext output = Executor.execute(node.bodyPublisher(), context);
            byteBufferPublisher = output.state(BodyPublisher.BODY_PUBLISHER_RESULT);
        } else {
            byteBufferPublisher = HttpRequest.BodyPublishers.noBody();
        }

        requestBuilder.POST(byteBufferPublisher);

        Response response = doRequest(input, requestTimeout, requestBuilder);

        return handleResponse(input, response);
    }

}
