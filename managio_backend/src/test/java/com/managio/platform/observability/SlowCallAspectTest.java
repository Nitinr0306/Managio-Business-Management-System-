package com.managio.platform.observability;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class SlowCallAspectTest {

    @Service
    static class PaymentsLike {
        public String record(String customerPhone) {
            return "ok:" + customerPhone;
        }

        public String quick() {
            return "fast";
        }

        public String failing() {
            throw new IllegalStateException("boom");
        }
    }

    private static PaymentsLike proxied(Duration threshold) {
        AspectJProxyFactory factory = new AspectJProxyFactory(new PaymentsLike());
        factory.setProxyTargetClass(true);
        factory.addAspect(new SlowCallAspect(threshold));
        return factory.getProxy();
    }

    @Test
    void warnsAboutSlowCalls(CapturedOutput output) {
        PaymentsLike service = proxied(Duration.ZERO);
        assertThat(service.quick()).isEqualTo("fast");
        assertThat(output).contains("Slow call: PaymentsLike.quick took");
    }

    @Test
    void neverLogsWhatWasPassedIn(CapturedOutput output) {
        PaymentsLike service = proxied(Duration.ZERO);
        assertThat(service.record("98765 43210")).isEqualTo("ok:98765 43210");
        assertThat(output).contains("Slow call: PaymentsLike.record took").doesNotContain("98765");
    }

    @Test
    void staysQuietForFastCalls(CapturedOutput output) {
        PaymentsLike service = proxied(Duration.ofSeconds(5));
        service.quick();
        assertThat(output).doesNotContain("Slow call");
    }

    @Test
    void timesFailingCallsAndLetsTheErrorThrough(CapturedOutput output) {
        PaymentsLike service = proxied(Duration.ZERO);
        assertThatThrownBy(service::failing).isInstanceOf(IllegalStateException.class).hasMessage("boom");
        assertThat(output).contains("Slow call: PaymentsLike.failing took");
    }
}
