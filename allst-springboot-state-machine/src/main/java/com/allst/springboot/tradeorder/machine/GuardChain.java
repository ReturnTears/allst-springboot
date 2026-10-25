package com.allst.springboot.tradeorder.machine;

import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;

/**
 * 把多个守卫串成一个。前面的守卫失败后，后面的守卫不再执行。
 */
public final class GuardChain {

    private GuardChain() {
    }

    @SafeVarargs
    public static <S, E> Guard<S, E> all(Guard<S, E>... guards) {
        return context -> {
            for (Guard<S, E> guard : guards) {
                if (!guard.evaluate(context)) {
                    return false;
                }
            }
            return true;
        };
    }

    public static void reject(StateContext<?, ?> context, String reason) {
        context.getExtendedState().getVariables().put("rejectReason", reason);
    }

    public static Object variable(StateContext<?, ?> context, String key) {
        return context.getExtendedState().getVariables().get(key);
    }

    public static String header(StateContext<?, ?> context, String key) {
        if (context.getMessageHeaders() == null) {
            return null;
        }
        Object value = context.getMessageHeaders().get(key);
        return value == null ? null : String.valueOf(value);
    }
}
