package org.deklanowski.eip.routing.aggregator.internal;

import com.google.common.base.Preconditions;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Predicate;
import org.apache.camel.builder.ExpressionClause;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.AggregateDefinition;
import org.apache.camel.model.FilterDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy;
import org.apache.commons.lang3.StringUtils;
import org.deklanowski.eip.spi.MessagePublisher;
import org.deklanowski.eip.spi.Transformer;
import org.deklanowski.eip.camel.MessagePublisherProcessor;

/**
 * This route transforms, aggregates messages and publishes arbitrary message types.
 * Transformed messages must be {@link java.io.Serializable}. If the input message
 * has headers then these are propagated with the transformed message.
 *
 * @author Declan Cox
 * @version $Id$
 * @since 01/10/2015
 */
public final class AggregatingRouter extends RouteBuilder {

    public static final Predicate NOOP_FILTER = exchange -> true;

    private final String routeFrom;
    private final String routeId;
    private final Transformer transformer;
    private final String filterExpression;
    private final AggregationStrategy aggregationStrategy;
    private final int completionSize;
    private final long completionIntervalMillis;
    private final MessagePublisher publisher;

    /**
     * Configurable aggregating router
     * @param routeId identifier for the route
     * @param routeFrom consuming uri
     * @param transformer {@link Transformer} implementation
     * @param filterExpression {@link org.apache.camel.language.Simple} expression used to filter messages.
     * @param aggregationStrategy {@link AggregationStrategy} how transformed messages are to be filtered
     * @param completionSize max number of elements in an aggregation
     * @param completionIntervalMillis max length of time and aggregation shall live
     * @param publisher publishing interface, publishes collections of messages of type O (see {@link Transformer}
     * @param <I> input message type
     * @param <O> transformed message type
     * @throws NullPointerException if any of the mandatory arguments is null
     * @throws IllegalArgumentException if arguments are found to be invalid
     */
    public <I, O> AggregatingRouter(String routeId,
                                    String routeFrom,
                                    Transformer<I, O> transformer,
                                    String filterExpression,
                                    AggregationStrategy aggregationStrategy,
                                    int completionSize,
                                    long completionIntervalMillis,
                                    MessagePublisher publisher) {

        Preconditions.checkArgument(StringUtils.isNotBlank(routeId),"Please specify a non-blank routeId");
        this.routeId = routeId;

        Preconditions.checkArgument(StringUtils.isNotBlank(routeFrom),"Endpoint URI is null or empty");
        this.routeFrom = routeFrom;

        this.transformer = Preconditions.checkNotNull(transformer,"Transformer may not be null");

        this.filterExpression = filterExpression;

        this.aggregationStrategy = Preconditions.checkNotNull(aggregationStrategy,"Aggregation strategy may not be null");

        Preconditions.checkArgument(completionSize > 0, "Negative completionSize specified");
        this.completionSize = completionSize;

        Preconditions.checkArgument(completionIntervalMillis > 0, "Negative completionIntervalMillis specified");
        this.completionIntervalMillis = completionIntervalMillis;

        this.publisher = Preconditions.checkNotNull(publisher,"Message publisher may not be null");;
    }

    /**
     * Configurable aggregating router with default {@link GroupedExchangeAggregationStrategy}
     * @param routeId identifier for the route
     * @param routeFrom consuming uri
     * @param transformer {@link Transformer} implementation
     * @param filterExpression {@link org.apache.camel.language.Simple} expression used to filter messages.
     * @param completionSize max number of elements in an aggregation
     * @param completionIntervalMillis max length of time and aggregation shall live
     * @param publisher publishing interface, publishes collections of messages of type O (see {@link Transformer}
     * @param <I> input message type
     * @param <O> transformed message type
     * @throws NullPointerException if any of the mandatory arguments is null
     * @throws IllegalArgumentException if arguments are found to be invalid
     */
    public <I, O> AggregatingRouter(String routeId,
                                    String routeFrom,
                                    Transformer<I, O> transformer,
                                    String filterExpression,
                                    int completionSize,
                                    long completionIntervalMillis,
                                    MessagePublisher publisher) {
        this(routeId, routeFrom, transformer, filterExpression, new GroupedExchangeAggregationStrategy(), completionSize, completionIntervalMillis, publisher);
    }


    @Override
    public void configure() throws Exception {

            from(this.routeFrom)
                    .tracing()
                    .routeId(this.routeId)
                    .bean(this.transformer)
                    .filter(simple(this.filterExpression))
                    .aggregate(constant(true), this.aggregationStrategy)
                    .completionSize(this.completionSize)
                    .completionInterval(this.completionIntervalMillis)
                    .process(new MessagePublisherProcessor(this.publisher)).id(getPublishId());
    }



    /**
     * Build a unique id for the terminal node of the route. All id's have to be unique
     * across deployed routes in karaf. The id allows us to test the route using
     * {@link org.apache.camel.builder.AdviceWithRouteBuilder} to non-invasively
     * weave in or add additional routing.
     *
     * @return id for the publish node.
     */
    private String getPublishId() {
       return this.routeId+"-publish";
    }
}
