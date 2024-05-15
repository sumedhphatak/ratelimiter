package com.demo.ratelimit.revised;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toUnmodifiableMap;

public abstract class AbstractRateLimiter {
    //internal non shared state unmodifiable map
    protected final Map<String, InternalRateLimitConfig> oneTimeRateLimitConfigMap;

    protected AbstractRateLimiter(Map<String, RateLimitConfig> inputMap) {
        this.oneTimeRateLimitConfigMap = inputMap.entrySet().stream()
                .collect(toUnmodifiableMap(Map.Entry::getKey, e ->
                        new InternalRateLimitConfig(e.getValue(),
                                Executors.newSingleThreadScheduledExecutor())));
    }

    protected void stop() {
        oneTimeRateLimitConfigMap.forEach((k, v) -> v.scheduledExecutorService().shutdown());
    }

    protected void start() {
        oneTimeRateLimitConfigMap.forEach((k, v) -> v.scheduledExecutorService().scheduleAtFixedRate(
                this::scheduledRunnable, 0L,
                computeFixedRatePeriodForMillis(v.rateLimitConfig()), TimeUnit.MILLISECONDS
        ));
    }

    protected abstract void scheduledRunnable();
    protected abstract RevisedApiResponse interceptApiCall(RevisedApiRequest apiRequest);
    protected abstract int availablePermits(String apiKey);

    protected long computeFixedRatePeriodForMillis(final RateLimitConfig config) {

        switch (config.timeUnit()) {
            case MINUTES -> {
                return (config.requestsPerDuration() / 60) * 1000L;
            }

            case SECONDS -> {
                return config.requestsPerDuration() * 1000L;
            }

            case MILLISECONDS -> {
                return config.requestsPerDuration();
            }

            default -> throw new IllegalArgumentException("not supported rate limit time unit");
        }
    }
}

record InternalRateLimitConfig(RateLimitConfig rateLimitConfig, ScheduledExecutorService scheduledExecutorService) {}
record RateLimitConfig(int requestsPerDuration, TimeUnit timeUnit) {}
record RevisedApiRequest(String apiKey, String url, Instant reqReceived) {}
record RevisedApiResponse(String apiKey, String requestUrl, Instant reqReceived, String response, int statusCode, int availablePermits){}

