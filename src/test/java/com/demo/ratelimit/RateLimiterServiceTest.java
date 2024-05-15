package com.demo.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    RateLimiterService subject;
    CountDownLatch countDownLatch = new CountDownLatch(1);

    @BeforeEach
    void setUp() {
        Map<String, RateLimitDescriptor> apiKeyRateLimitMap = new HashMap<>();
        apiKeyRateLimitMap.put("apiKey1", new RateLimitDescriptor(60, TimeUnit.MINUTES));
        subject = new RateLimiterService(apiKeyRateLimitMap);
    }

    @Test
    void test_rateLimit() throws InterruptedException {

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                subject.apiRequest(
                        new ApiRequest("apiKey1", "http://rateLimitingUrl", Instant.now())
                );
            } catch(RuntimeException e) {
                countDownLatch.countDown();
            }

        }, 0L, 200, TimeUnit.MILLISECONDS);
        countDownLatch.await(60, TimeUnit.SECONDS);
    }
}