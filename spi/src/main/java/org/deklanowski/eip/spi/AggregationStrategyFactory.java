package org.deklanowski.eip.spi;

import org.apache.camel.processor.aggregate.AggregationStrategy;

/**
 * Aggregation strategy factory SPI. Implement one of these for your
 * strategy and register it as a service.
 *
 * @author Declan Cox
 * @version $Id$
 * @since 15/10/2015
 */
public interface AggregationStrategyFactory {

    /**
     * Create an instance.
     * @return {@link AggregationStrategy} instance.
     */
    AggregationStrategy create();

}
