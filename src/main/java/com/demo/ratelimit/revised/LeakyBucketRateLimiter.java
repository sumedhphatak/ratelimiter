package com.demo.ratelimit.revised;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.time.Instant.now;

@Slf4j
public class LeakyBucketRateLimiter extends AbstractRateLimiter {

    private final Map<String, List<RevisedApiRequest>> leakyBucketPerApikey;

    //one time config
    public LeakyBucketRateLimiter(Map<String, RateLimitConfig> inputMap) {
        super(inputMap);
        this.leakyBucketPerApikey = new HashMap<>();
        oneTimeRateLimitConfigMap.forEach((k, v) -> {
            List<RevisedApiRequest> emptyPermits = IntStream.range(0, 30).mapToObj(i -> new RevisedApiRequest(k, "EmptyUrl",
                    now())).collect(Collectors.toList());
            this.leakyBucketPerApikey.put(k, Collections.synchronizedList(emptyPermits));
        });
    }

    @Override
    public void scheduledRunnable() {
        oneTimeRateLimitConfigMap.forEach((k, v) -> {
            if (!this.leakyBucketPerApikey.get(k).isEmpty()) {
                this.leakyBucketPerApikey.get(k).remove(0);
            }
        });
    }

    @Override
    public RevisedApiResponse interceptApiCall(RevisedApiRequest apiRequest) {
        int requestsPerDuration = oneTimeRateLimitConfigMap.get(apiRequest.apiKey()).rateLimitConfig().requestsPerDuration();
        List<RevisedApiRequest> leakyBucketPerApi = leakyBucketPerApikey.get(apiRequest.apiKey());
        if (leakyBucketPerApi.size() == requestsPerDuration) {

            log.error("Rate Limit Exceeded 429");
            return new RevisedApiResponse(apiRequest.apiKey(), apiRequest.url(), apiRequest.reqReceived(), "429 Rate Limit Exceeded", 429, 0);
        }
        leakyBucketPerApi.add(apiRequest);
        log.info("Execute Api Request: {}", apiRequest);

        return new RevisedApiResponse(apiRequest.apiKey(), apiRequest.url(), apiRequest.reqReceived(), "200 OK, ResponseOk", 200, requestsPerDuration - leakyBucketPerApi.size());
    }

    @Override
    public int availablePermits(String apiKey) {
        int requestsPerDuration = oneTimeRateLimitConfigMap.get(apiKey).rateLimitConfig().requestsPerDuration();
        List<RevisedApiRequest> leakyBucketPerApi = leakyBucketPerApikey.get(apiKey);

        return requestsPerDuration - leakyBucketPerApi.size();
    }

}


