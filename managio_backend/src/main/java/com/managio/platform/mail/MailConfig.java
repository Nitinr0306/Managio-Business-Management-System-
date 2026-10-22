package com.managio.platform.mail;

import com.managio.platform.config.ManagioProperties;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Chooses Resend when an API key is configured and a logging mailer otherwise (D-017). Resend is called on a small
 * background pool, so a request never waits for the email provider: a slow provider can't hold requests up, and a
 * password-reset request takes the same time whether or not the account exists.
 */
@Slf4j
@Configuration
class MailConfig {

    @Bean
    @ConditionalOnMissingBean(Mailer.class)
    Mailer mailer(ManagioProperties properties, JsonMapper jsonMapper) {
        if (!properties.mail().delivers()) {
            log.warn("RESEND_API_KEY is not set; emails will be logged instead of sent");
            boolean logContent = properties.mail().logContent();
            return message -> log.info("Email (not sent) to={} subject=\"{}\"{}", message.to(), message.subject(),
                    logContent ? "\n" + message.text() : "");
        }
        log.info("Emails are sent through Resend from {}", properties.mail().from());
        return new BackgroundMailer(new ResendMailer(properties.mail().resendApiKey(), properties.mail().from(), jsonMapper));
    }

    /**
     * Hands each email to a small pool. Not a Spring executor bean, so it never replaces the application's own task
     * executor. Shutdown waits for queued emails.
     */
    static final class BackgroundMailer implements Mailer, DisposableBean {

        private final Mailer delegate;
        private final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        BackgroundMailer(Mailer delegate) {
            this.delegate = delegate;
            executor.setThreadNamePrefix("mail-");
            executor.setCorePoolSize(2);
            executor.setMaxPoolSize(4);
            executor.setQueueCapacity(500);
            // A full queue sends on the caller's thread rather than dropping the email.
            executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
            executor.setWaitForTasksToCompleteOnShutdown(true);
            executor.setAwaitTerminationSeconds(20);
            executor.setTaskDecorator(task -> {
                Map<String, String> context = MDC.getCopyOfContextMap();
                return () -> {
                    if (context != null) {
                        MDC.setContextMap(context);
                    }
                    try {
                        task.run();
                    } finally {
                        MDC.clear();
                    }
                };
            });
            executor.initialize();
        }

        @Override
        public void send(MailMessage message) {
            executor.execute(() -> delegate.send(message));
        }

        @Override
        public void destroy() {
            executor.shutdown();
        }
    }

    /** Sends through the Resend HTTP API. Failures are logged and never propagate to the caller. */
    static final class ResendMailer implements Mailer {

        private static final URI ENDPOINT = URI.create("https://api.resend.com/emails");

        private final String apiKey;
        private final String from;
        private final JsonMapper jsonMapper;
        private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

        ResendMailer(String apiKey, String from, JsonMapper jsonMapper) {
            this.apiKey = apiKey;
            this.from = from;
            this.jsonMapper = jsonMapper;
        }

        @Override
        public void send(MailMessage message) {
            String body = jsonMapper.writeValueAsString(Map.of(
                    "from", from,
                    "to", List.of(message.to()),
                    "subject", message.subject(),
                    "html", message.html(),
                    "text", message.text()));
            HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 300) {
                    log.error("Resend rejected email subject=\"{}\": HTTP {} {}", message.subject(),
                            response.statusCode(), response.body());
                }
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                log.error("Could not send email subject=\"{}\"", message.subject(), e);
            }
        }
    }
}
