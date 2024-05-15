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
public class FixedTokenBucketRateLimiter extends AbstractRateLimiter{

    private final Map<String, List<RevisedApiRequest>> fixedTokenBucketPerApikey;

    //one time config
    public FixedTokenBucketRateLimiter(Map<String, RateLimitConfig> inputMap) {
       super(inputMap);
        this.fixedTokenBucketPerApikey = new HashMap<>();
        oneTimeRateLimitConfigMap.forEach((k, v) -> {
            List<RevisedApiRequest> emptyPermits = IntStream.range(0, 30).mapToObj(i -> new RevisedApiRequest(k, "EmptyUrl",
                    now())).collect(Collectors.toList());
            this.fixedTokenBucketPerApikey.put(k, Collections.synchronizedList(emptyPermits));
        });
    }

    @Override
    public void scheduledRunnable() {
        oneTimeRateLimitConfigMap.forEach((k, v) -> this.fixedTokenBucketPerApikey.get(k)
                .add(new RevisedApiRequest(k, "EmptyUrl", now())));
    }

    @Override
    public RevisedApiResponse interceptApiCall(RevisedApiRequest apiRequest) {
        List<RevisedApiRequest> fixedTokenBucket = fixedTokenBucketPerApikey.get(apiRequest.apiKey());
        if (fixedTokenBucket.isEmpty()) {
            log.error("Rate Limit Exceeded 429");
            return new RevisedApiResponse(apiRequest.apiKey(), apiRequest.url(), apiRequest.reqReceived(), "429 Rate Limit Exceeded", 429, 0);
        }
        fixedTokenBucket.remove(0);
        log.info("Execute Api Request: {}", apiRequest);
        return new RevisedApiResponse(apiRequest.apiKey(), apiRequest.url(), apiRequest.reqReceived(), "200 OK, ResponseOk", 200, fixedTokenBucket.size());
    }

    @Override
    protected int availablePermits(String apiKey) {
        return fixedTokenBucketPerApikey.get(apiKey).size();
    }

}


