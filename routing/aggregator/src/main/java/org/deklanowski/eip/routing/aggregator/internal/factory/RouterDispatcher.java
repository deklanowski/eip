package org.deklanowski.eip.routing.aggregator.internal.factory;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.deklanowski.eip.routing.aggregator.internal.AggregatingRouter;
import org.deklanowski.eip.spi.MessagePublisher;
import org.deklanowski.eip.spi.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The RouterDispatcher manages the construction, starting and stopping of a route.
 *
 * @author Declan Cox
 * @since 1.0
 */
public class RouterDispatcher {

    private Logger logger = LoggerFactory.getLogger(RouterDispatcher.class);

    private final String routeId;
    private final String fromUri;
    private final Transformer<?, ?> transformer;
    private final String filterExpression;
    private final AggregationStrategy aggregationStrategy;
    private final int completionSize;
    private final long completionIntervalMillis;
    private final MessagePublisher publisher;
    private final String deadLetterChannel;
    private final CamelContext camelContext;

    private RouteBuilder routeBuilder;


    public RouterDispatcher(String routeId,
                            String fromUri,
                            Transformer<?, ?> transformer,
                            String filterExpression,
                            AggregationStrategy aggregationStrategy,
                            int completionSize,
                            long completionIntervalMillis,
                            MessagePublisher publisher,
                            String deadLetterChannel,
                            CamelContext camelContext) {
        this.routeId = routeId;
        this.fromUri = fromUri;
        this.transformer = transformer;
        this.filterExpression = filterExpression;
        this.aggregationStrategy = aggregationStrategy;
        this.completionSize = completionSize;
        this.completionIntervalMillis = completionIntervalMillis;
        this.publisher = publisher;
        this.deadLetterChannel = deadLetterChannel;
        this.camelContext = camelContext;
    }

    public void start() {
        try {
            routeBuilder = buildRouter();
            logger.info("Route " + routeBuilder + " starting...");
            camelContext.start();
            camelContext.addRoutes(routeBuilder);
        } catch (Exception ex) {
            logger.error("Failed to get route {} off the ground, something's just not right Jim {}", this.routeId, ex);
        }
    }


    /**
     * Remove the route from the camel context.
     */
    public void stop() {
        if (routeBuilder != null) {
            try {
                camelContext.removeRoute(routeBuilder.toString());
            } catch (Exception e) {
                logger.error("Could not remove route " + routeBuilder + " " + e);
            }
        }
    }

    /**
     * Build that sucka !
     *
     * @return {@link AggregatingRouter} instance
     * @throws Exception
     */
    protected RouteBuilder buildRouter() throws Exception {
        return new AggregatingRouter(
                this.routeId,
                this.fromUri,
                this.transformer,
                this.filterExpression,
                this.aggregationStrategy,
                this.completionSize,
                this.completionIntervalMillis,
                this.publisher,
                this.deadLetterChannel);
    }
}
