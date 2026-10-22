package com.managio.platform.observability;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Logs a warning when a service call takes longer than the threshold (default 500 ms), naming the method. The
 * request ID is already on every log line (RequestIdFilter), so a slow call can be traced to its request.
 * Arguments and results are never logged: they can hold customers' personal data.
 */
@Slf4j
@Aspect
@Component
public class SlowCallAspect {

    private final long thresholdNanos;

    public SlowCallAspect(@Value("${managio.slow-call-threshold:500ms}") Duration threshold) {
        this.thresholdNanos = threshold.toNanos();
    }

    @Around("within(com.managio..*) && @within(org.springframework.stereotype.Service) && execution(public * *(..))")
    public Object timeServiceCall(ProceedingJoinPoint call) throws Throwable {
        long start = System.nanoTime();
        try {
            return call.proceed();
        } finally {
            long elapsed = System.nanoTime() - start;
            if (elapsed >= thresholdNanos) {
                log.warn("Slow call: {}.{} took {} ms", call.getSignature().getDeclaringType().getSimpleName(),
                        call.getSignature().getName(), elapsed / 1_000_000);
            }
        }
    }
}
