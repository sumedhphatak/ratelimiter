package com.demo.ratelimit;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class RateLimiterService {

    //internal non shared state
    //unmodifiable map
    private final Map<String, RateLimitDescriptor> apiKeyRateLimitMap;
    private final Map<String, List<ApiRequest>> permitsPerApiKey;

    //one time config
    public RateLimiterService(Map<String, RateLimitDescriptor> apiKeyRateLimitMap) {
        this.apiKeyRateLimitMap = new HashMap<>();
        this.apiKeyRateLimitMap.putAll(apiKeyRateLimitMap);
        this.permitsPerApiKey = new HashMap<>();

        apiKeyRateLimitMap.forEach((k, v) -> {
            if(v.timeUnit() == TimeUnit.MINUTES) {
                List<ApiRequest> emptyPermits = IntStream.range(0, 30).mapToObj(i -> new ApiRequest(k, "EmptyUrl", Instant.now())).collect(Collectors.toList());
                this.permitsPerApiKey.put(k, Collections.synchronizedList(emptyPermits));
                ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
                scheduledExecutorService.scheduleAtFixedRate(() -> this.permitsPerApiKey.get(k).add(new ApiRequest(k, "EmptyUrl", Instant.now())),
                        0L, v.requestPerDuration()/60, TimeUnit.SECONDS);
            }
        });
    }

    public void apiRequest(ApiRequest apiRequest) {
        if(permitsPerApiKey.get(apiRequest.apiKey()).isEmpty()) {
            log.error("Rate Limit Exceeded 429");
            throw new RuntimeException("Rate Limit Exceeded 429");
        }
        permitsPerApiKey.get(apiRequest.apiKey()).remove(0);
        log.info("Execute Api Request: {}", apiRequest);
    }

}
record RateLimitDescriptor(int requestPerDuration, TimeUnit timeUnit){};

record ApiRequest(String apiKey, String url, Instant reqReceived){};
