package io.descoped.dc.core.handler;

import io.descoped.dc.api.PageContext;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.el.ExpressionLanguage;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.node.Execute;
import io.descoped.dc.api.node.Paginate;
import io.descoped.dc.core.executor.Executor;
import io.descoped.dc.core.health.HealthWorkerMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

@Handler(forClass = Paginate.class)
public class PaginateHandler extends AbstractNodeHandler<Paginate> {

    static final String ADD_PAGE_CONTENT = "ADD_PAGE_CONTENT";
    static final String ADD_PAGE_CONTENT_TO_POSITION = "ADD_PAGE_CONTENT_TO_POSITION";
    private static final Logger LOG = LoggerFactory.getLogger(PaginateHandler.class);


    public PaginateHandler(Paginate node) {
        super(node);
    }

    /**
     * The Worker manages the pagination lifecycle
     */
    @Override
    public ExecutionContext execute(ExecutionContext context) {
        evaluateGlobalContext(context);

        try {
            if (context.services().contains(HealthWorkerMonitor.class)) {
                context.services().get(HealthWorkerMonitor.class).request().setPrefetchThreshold(node.threshold());
            }
            PaginationLifecycle lifecycle = new PaginationLifecycle(node.threshold(), this);
            return lifecycle.start(context);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void evaluateGlobalContext(ExecutionContext context) {
        // copy global-context and merge services from paginate-context
        ExecutionContext globalContext = new ExecutionContext.Builder()
                .of(node.configurations().flowContext().globalContext())
                .services(context)
                .build();

        // evaluate expression given that there is an identifier that matches
        Map<String, Object> globalVariables = new LinkedHashMap<>(globalContext.variables());
        for (String variableName : globalContext.variables().keySet()) {
            ExpressionLanguage el = new ExpressionLanguage(globalContext);
            Object variableValue = globalContext.variable(variableName);

            // evaluate global variable expression
            if (el.isExpression(String.valueOf(variableValue))) {
                LOG.info("Evaluate global configuration variable: {} => {}", variableName, variableValue);
                Object evalValue = el.evaluateExpression(String.valueOf(variableValue));
                globalVariables.put(variableName, evalValue);
            } else {
                globalVariables.put(variableName, variableValue);
            }
        }

        // prepare a fresh evaluated global-context
        ExecutionContext evaluatedGlobalContext = new ExecutionContext.Builder()
                .of(node.configurations().flowContext().globalContext())
                .services(context)
                .variables(globalVariables)
                .build();

        // merge global with paginate-context
        context.merge(evaluatedGlobalContext);
    }

    /**
     * Execute targets
     */
    public ExecutionContext doPage(ExecutionContext input) {
        ExecutionContext output = ExecutionContext.of(input);

        for (Execute target : node.targets()) {
            ExecutionContext targetInput = ExecutionContext.of(input);

            // merge input variables with node variables
            node.variableNames().forEach(name -> {
                if (targetInput.variable(name) == null) {
                    String variableValue = node.variable(name);
                    targetInput.variable(name, variableValue);
                }
            });

            // evaluate expression given that there is an identifier that matches
            for (String variableName : node.variableNames()) {
                ExpressionLanguage el = new ExpressionLanguage(input);
                String elExpr = node.variable(variableName);
                if (el.isExpression(elExpr) && input.variables().containsKey(el.getExpression(elExpr))) {
                    Object elValue = el.evaluateExpression(elExpr);
                    targetInput.variables().put(variableName, elValue);
                }
            }

            targetInput.state(ADD_PAGE_CONTENT, node.addPageContent());
            targetInput.state(ADD_PAGE_CONTENT_TO_POSITION, node.positionVariable());

            // add correlation-id on fan-out
            //CorrelationIds.of(targetInput).add();

            ExecutionContext targetOutput = Executor.execute(target, targetInput);

            // merge returned variables
            output.merge(targetOutput);

            // TODO fix keep the previous page correlation-id reference
            //CorrelationIds.create(input).tail(CorrelationIds.of(targetInput));
        }

        // set end-of-stream if until-condition is met
        if (Conditions.untilCondition(node.condition(), output)) {
            PageContext pageContext = output.state(PageContext.class);
            if (pageContext == null) {
                pageContext = new PageContext.Builder()
                        .addNextPosition(node.positionVariable(), null)
                        .build();
            }
            pageContext.setEndOfStream(true);
            output.state(PageContext.class, pageContext);

            LOG.trace("Until condition satisfied, setting end-of-stream. {} with returnVariables={}", evalCondition(output), pageContext.nextPositionMap());
        }

        return output;
    }

    private Object evalCondition(ExecutionContext output) {
        ExpressionLanguage el = new ExpressionLanguage(output);
        String expr = node.condition().identifier();
        if (el.isExpression(node.condition().identifier())) {
            String result = el.evaluateExpressions(expr);
            return String.format("Expression: [\"%s\" = %s]", expr, result);
        } else {
            return String.format("Expression: [\"%s\" = %s]", expr, output.variable(node.condition().identifier()));
        }
    }

}
