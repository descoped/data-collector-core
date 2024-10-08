package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.handler.QueryException;
import io.descoped.dc.api.handler.QueryState;
import io.descoped.dc.api.node.RegEx;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Handler(forClass = RegEx.class)
public class RegExHandler extends AbstractQueryHandler<RegEx> {

    static final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();

    public RegExHandler(RegEx node) {
        super(node);
    }

    @Override
    public ExecutionContext execute(ExecutionContext input) {
        QueryState<RegEx> queryState = input.state(QueryState.class);

        if (queryState == null) {
            throw new IllegalArgumentException("QueryState is not set!");
        }

        if (queryState.type() != Type.STRING_LITERAL) {
            throw new QueryException("Only QueryFeature.Type.STRING_LITERAL is supported!");
        }

        /*
         * execute sub-query and get regex matcher token
         */

        String result = Queries.from(input, node.query()).evaluateStringLiteral(queryState.data());

        /*
         * execute this handler
         */

        ExecutionContext regexContext = ExecutionContext.of(input);
        regexContext.state(QueryState.class, new QueryState<>(queryState.type(), result));

        return super.execute(regexContext);
    }

    @Override
    public List<?> evaluateList(Object data) {
        throw new UnsupportedOperationException("queryList is not supported!");
    }

    @Override
    public Object evaluateObject(Object data) {
        throw new UnsupportedOperationException("queryObject is not supported!");
    }

    @Override
    public String evaluateStringLiteral(Object data) {
        String text = (data instanceof byte[]) ? new String((byte[]) data, StandardCharsets.UTF_8) : (String) data;
        try {
            Pattern pattern = patternCache.computeIfAbsent(node.expression(), Pattern::compile);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String value = matcher.group(0);
                return value;
            }

        } catch (PatternSyntaxException e) {
            throw new QueryException("Error parsing: " + data, e);
        }
        return null;
    }
}
