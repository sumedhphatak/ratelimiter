Offer different APIs to sellers, and we want to prevent misuse by implementing a basic rate limiting system for one of the API endpoints. The objective is to limit the number of requests a seller can make to the API within a specific time frame.
Requirements:
Design a rate limiter that allows a user to make a maximum of N requests within a rolling time window of M seconds.
The rate limiter should be able to handle a large number of users concurrently.
Ensure that the implementation is thread-safe and can be used in a multi-threaded environment.


> Rate Limiter
> > Nouns:
>  > - Rate limit (Requests per minute, N requests per min) 
>  > - ApiKeys (for different clients)
>  > - RateLimit URLs (set of URLs to rate limit)
>  > - Rate Limit exceeds: respond back 429s (rate limit exceeded)
>  > - Prefered API Clients could get more requests per min (more rate limit)

> Algorithms:
> -  Fixed Token Bucket (60 rpm - 1 req per sec)
> -  Leaky Token Bucket (remove a token/permit every sec)
> - Sliding Windows Logs (maintain a time sorted queue)
> - Sliding Window Counters