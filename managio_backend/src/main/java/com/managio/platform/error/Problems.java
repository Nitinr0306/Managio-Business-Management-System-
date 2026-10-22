package com.managio.platform.error;

import com.managio.platform.web.RequestIdFilter;
import org.slf4j.MDC;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.Map;

/** Builds the problem-detail bodies described in docs/API.md#errors. */
public final class Problems {

    private static final String TYPE_BASE = "https://managio.app/problems/";

    private Problems() {
    }

    public static ProblemDetail of(HttpStatusCode status, String code, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(TYPE_BASE + code.replace('.', '-')));
        problem.setTitle(title);
        problem.setProperty("code", code);
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        if (requestId != null) {
            problem.setProperty("requestId", requestId);
        }
        return problem;
    }

    public static ProblemDetail withErrors(ProblemDetail problem, Map<String, String> errors) {
        if (!errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
        return problem;
    }
}
