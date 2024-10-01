package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;

public class ExecutionWrapper {

    @RuntimeType
    public static ExecutionContext intercept(@Origin Method method, @This Object self, @Argument(0) ExecutionContext input) throws Exception {
        System.out.printf("intercepted%n");
        ExecutionContext result = (ExecutionContext) method.invoke(input);
        return result;
    }

    @RuntimeType
    public static ExecutionContext execute(@Origin Method method, @This Object self, @Argument(0) ExecutionContext input) {
        System.out.printf("Intercepted%n");
        return null;
    }

}
