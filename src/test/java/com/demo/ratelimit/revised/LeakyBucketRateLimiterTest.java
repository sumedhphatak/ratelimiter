package com.demo.ratelimit.revised;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class LeakyBucketRateLimiterTest {
    LeakyBucketRateLimiter subject;
    CountDownLatch countDownLatch = new CountDownLatch(1);

    @BeforeEach
    void setUp() {
        Map<String, RateLimitConfig> apiKeyRateLimitMap = new HashMap<>();
        apiKeyRateLimitMap.put("apiKey1", new RateLimitConfig(60, TimeUnit.MINUTES));
        subject = new LeakyBucketRateLimiter(apiKeyRateLimitMap);
    }

    @AfterEach
    void tearDown() {
        subject.stop();
    }

    @Test
    void test_rateLimit_200() throws InterruptedException {

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        subject.start();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            RevisedApiResponse response = subject.interceptApiCall(
                    new RevisedApiRequest("apiKey1", "http://rateLimitingUrl", Instant.now())
            );
            log.info("apiResponse: {}", response);
            assertEquals(200, response.statusCode());
            if (response.statusCode() == 429) {
                log.info("test abruptly finished due to 429");
                countDownLatch.countDown();
            }

        }, 0L, 900, TimeUnit.MILLISECONDS);
        countDownLatch.await(60, TimeUnit.SECONDS);
        if (countDownLatch.getCount() != 0) {
            log.info("test finished");
        }
    }

    @Test
    void test_rateLimit_429() throws InterruptedException {

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        subject.start();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            RevisedApiResponse response = subject.interceptApiCall(
                    new RevisedApiRequest("apiKey1", "http://rateLimitingUrl", Instant.now())
            );
            log.info("apiResponse: {}", response);
            if (response.statusCode() == 429) {
                assertEquals(429, response.statusCode());
                log.info("test finished due to 429");
                countDownLatch.countDown();
            }

        }, 0L, 200, TimeUnit.MILLISECONDS);
        countDownLatch.await(60, TimeUnit.SECONDS);
        if (countDownLatch.getCount() != 0) {
            log.info("test finished");
        }
    }
}