package com.buyapp.apigateway.filter;

@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private final Logger logger = LoggerFactory.getLogger(RequestLogginfFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        logger.info("Incoming request: {} {} from {}",
                request.getMethod(),
                request.getURI(),
                request.getRemoteAddress());

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            logger.info("Response status: {} for {} {}",
                    response.getStatusCode(),
                    response.getMethod(),
                    request.getURI());
        }));
    }

    @Override
    public int getOrder() {
        return -1; // High precedence
    }
}