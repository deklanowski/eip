package org.deklanowski.eip.routing.aggregator.internal.factory;

import com.google.common.base.Preconditions;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy;
import org.apache.commons.lang3.StringUtils;
import org.deklanowski.eip.camel.CamelMessagePublisher;
import org.deklanowski.eip.routing.aggregator.internal.registry.ServiceRegistry;
import org.deklanowski.eip.routing.aggregator.internal.registry.ServiceRegistryImpl;
import org.deklanowski.eip.spi.AggregationStrategyFactory;
import org.deklanowski.eip.spi.MessagePublisher;
import org.deklanowski.eip.spi.Transformer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@SuppressWarnings("unchecked")
public class RouterManagedServiceFactory implements ManagedServiceFactory {

    private final Logger logger = LoggerFactory.getLogger(RouterManagedServiceFactory.class);

    private static final String ROUTE_ID = "route.id";
    private static final String ROUTE_FROM_URI = "route.from.uri";
    private static final String ROUTE_TO_URI = "route.to.uri";
    private static final String ROUTE_FILTER_EXPRESSION = "route.filter.expression";
    private static final String ROUTE_PUBLISHER_TYPE = "route.publisher.type";
    private static final String ROUTE_COMPLETION_SIZE = "route.completion-size";
    private static final String ROUTE_COMPLETION_INTERVAL_MILLIS = "route.completion-interval-millis";
    private static final String ROUTE_TRANSFORMER_TYPE = "route.transformer.type";
    private static final String ROUTE_AGGREGATION_TYPE = "route.aggregation.type";
    private static final String ROUTE_DEADLETTER_URI = "route.deadletter.uri";


    /**
     * Track {@link RouterDispatcher}instances we have created.
     */
    private Map<String, RouterDispatcher> dispatchers = Collections.synchronizedMap(new HashMap<>());

    /** Map all registered {@link AggregationStrategy} instances */
    private ServiceRegistry<AggregationStrategyFactory> strategies = new ServiceRegistryImpl<>("type");


    /** Map all registered {@link Transformer} instances */
    private ServiceRegistry<Transformer<?,?>> transformers = new ServiceRegistryImpl<>("type");

    /** Map all registered {@link MessagePublisher} instances */
    private ServiceRegistry<MessagePublisher> messagePublishers = new ServiceRegistryImpl<>("type");



    private BundleContext bundleContext;
    private CamelContext camelContext;
    private ServiceRegistration registration;
    private ServiceTracker tracker;
    private String configurationPid;


    /**
     * A human readable name for our factory
     *
     * @return factory name
     */
    @Override
    public String getName() {
        return "AggregatingRouter factory";
    }



    /**
     * Create new {@link RouterDispatcher} instance or update configuration of an
     * existing one (this amounts to stopping the old instance and creating a new
     * one with the same pid and updated configuration).
     *
     * @param pid managed service factory instance pid
     * @param properties router configuration
     * @throws ConfigurationException in case of invalid properties.
     */
    @Override
    public void updated(String pid,
                        Dictionary<String, ?> properties) throws ConfigurationException {

        RouterDispatcher newDispatcher = null;

        try {
            validateConfiguration(properties);
            newDispatcher = buildRouterDispatcher(properties);
        } finally {

            RouterDispatcher oldDispatcher = (newDispatcher == null) ? dispatchers.remove(pid) : dispatchers.put(pid, newDispatcher);

            if (oldDispatcher != null) {
                oldDispatcher.stop();
            }

            if (newDispatcher != null) {
                logger.debug("Starting router engine...");
                newDispatcher.start();
            }

        }

    }



    public void registerTransformer(Transformer<?,?> transformer, Map<String, Object> properties) {
        transformers.register(transformer,properties);
    }

    public void unregisterTransformer(Transformer<?,?> transformer, Map<String, Object> properties) {
        transformers.unregister(transformer, properties);
    }


    public void registerAggregationStrategyFactory(AggregationStrategyFactory factory, Map<String, Object> properties) {
        strategies.register(factory, properties);
    }

    public void unregisterAggregationStrategyFactory(AggregationStrategyFactory factory, Map<String, Object> properties) {
        strategies.unregister(factory, properties);
    }

    public void registerMessagePublisher(MessagePublisher publisher,
                                         Map<String, Object> properties) {
        messagePublishers.register(publisher, properties);
    }

    public void unregisterMessagePublisher(MessagePublisher publisher,
                                           Map<String, Object> properties) {
        messagePublishers.unregister(publisher, properties);
    }

    /**
     * Check configuration properties.
     *
     * @param properties property dispatchers
     * @throws ConfigurationException if null or badly formatted properties are found
     */
    private void validateConfiguration(Dictionary<String, ?> properties) throws ConfigurationException {

        String value = null;
        String key = null;

        try {
            key = ROUTE_ID;
            value = validateProperty(key, properties);

            key = ROUTE_FROM_URI;
            value = validateProperty(key, properties);

            key = ROUTE_COMPLETION_SIZE;
            value = validateProperty(key, properties);

            Integer.parseInt(value);

            key = ROUTE_COMPLETION_INTERVAL_MILLIS;
            value = validateProperty(key, properties);
            Long.parseLong(value);


        } catch (NumberFormatException e) {
            throw new ConfigurationException(key,"'"+value + "' is not a number", e);
        }
    }

    /**
     * Checks that a <em>mandatory</em> property is present and non-empty otherwise throws and exception
     * @param key property key
     * @param properties property dispatchers
     * @return value if non-empty
     * @throws ConfigurationException if null of empty value encountered
     */
    private String validateProperty(String key,
                                    Dictionary<String, ?> properties) throws ConfigurationException {
        String value = (String) properties.get(key);
        if (StringUtils.isEmpty(value)) {
            throw new ConfigurationException(key, " mandatory field");
        }
        return value;
    }


    @Override
    public void deleted(String pid) {

        RouterDispatcher engine = dispatchers.remove(pid);

        if (engine != null) {
            engine.stop();
        }

        logger.info("RouterDispatcher deleted " + pid);
    }


    /**
     * Called by blueprint. This is configured in the blueprint xml configuration and is
     * an alternative to having a custom bundle activator.
     */
    public void init() {
        logger.info("Starting " + this.getName());
        Dictionary servProps = new Properties();
        servProps.put(Constants.SERVICE_PID, configurationPid);
        registration = bundleContext.registerService(ManagedServiceFactory.class.getName(), this, servProps);
        tracker = new ServiceTracker(bundleContext, ConfigurationAdmin.class.getName(), null);
        tracker.open();
        logger.info("Started " + this.getName());
    }

    /**
     * Called by blueprint. This is configured in the blueprint xml configuration and is
     * an alternative to having a custom bundle activator.
     */
    public void destroy() {
        logger.info("Destroying AggregatingRouter factory " + configurationPid);
        registration.unregister();
        tracker.close();
    }


    public void setConfigurationPid(String configurationPid) {
        this.configurationPid = configurationPid;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }


    /**
     * Build an {@link RouterDispatcher} instance
     *
     * @param properties validated properties
     * @return engine instance
     */
    private RouterDispatcher buildRouterDispatcher(Dictionary<String, ?> properties) {

        RouterDispatcher dispatcher;

        dispatcher = new RouterDispatcher(
                getRouteId(properties),
                (String) properties.get(ROUTE_FROM_URI),
                getTransformer(properties),
                getFilterExpression(properties),
                getAggregationStrategy(properties), Integer.parseInt((String) properties.get(ROUTE_COMPLETION_SIZE)),
                Long.parseLong((String) properties.get(ROUTE_COMPLETION_INTERVAL_MILLIS)),
                getMessagePublisher(properties),
                getDeadLetterChannel(properties),
                camelContext);

        return dispatcher;
    }


    private String getRouteId(Dictionary<String, ?> properties) {
        return (String) properties.get(ROUTE_ID);
    }


    private String getFilterExpression(Dictionary<String, ?> properties) {
        return (String) properties.get(ROUTE_FILTER_EXPRESSION);
    }


    /**
     * Get a transformer instance
     * @param properties route properties
     * @return {@link Transformer} instance of the desired type
     * @throws NullPointerException if a transformer of the specified type is not registered.
     */
    private Transformer<?, ?> getTransformer(Dictionary<String, ?> properties) {
        String type = (String) properties.get(ROUTE_TRANSFORMER_TYPE);
        type = Preconditions.checkNotNull(type,"Transformer service key not specified, can't go on");

        Transformer<?,?> transformer = transformers.get(type);
        Preconditions.checkNotNull(transformer,"Transformer instance of type"+type+" not registered, can't go on");
        return transformer;
    }



    /**
     * Create an aggregation strategy with the selected factory. If no factory of the specifed type
     * is found the default to a {@link GroupedExchangeAggregationStrategy}
     * @param properties route properties
     * @return {@link AggregationStrategy} instance
     */
    private AggregationStrategy getAggregationStrategy(final Dictionary<String, ?> properties) {

        final String type = (String) properties.get(ROUTE_AGGREGATION_TYPE);

        AggregationStrategy strategy = null;

        if (type != null) {
            AggregationStrategyFactory strategyFactory = strategies.get(type);
            if (strategyFactory != null) {
                strategy = strategyFactory.create();
            } else {
                logger.debug("AggregationStrategy of type {} not registered, selecting default strategy", type);
            }
        } else {
            logger.debug("AggregationStrategy service key not specified, will use default GroupedExchangeAggregationStrategy");
        }

        return strategy == null ? new GroupedExchangeAggregationStrategy() : strategy;
    }


    /**
     * Get destination publisher instance. If there is a registered instance of the
     * desired type use it otherwise check it a camel uri was configured and create
     * a publisher instance backed by a {@link ProducerTemplate}.
     * @param properties route properties
     * @return {@link MessagePublisher} instance
     * @throws NullPointerException if it was not possible to choose a destination.
     */
    private MessagePublisher getMessagePublisher(Dictionary<String, ?> properties) {
        String publisherServiceKey = (String) properties.get(ROUTE_PUBLISHER_TYPE);
        String routeToUri = (String) properties.get(ROUTE_TO_URI);

        MessagePublisher publisher = null;

        if (StringUtils.isNotBlank(publisherServiceKey)) {
            publisher = messagePublishers.get(publisherServiceKey);
        }

        if (publisher == null) {
            logger.debug("No registered publisher of type {} found",publisherServiceKey);
            if (routeToUri != null) {
                logger.debug("Creating CamelEventPublisher instance with route destination {}",routeToUri);
                ProducerTemplate producerTemplate = camelContext.createProducerTemplate();
                publisher = new CamelMessagePublisher(producerTemplate);
                producerTemplate.setDefaultEndpointUri(routeToUri);
            }
        }

        Preconditions.checkNotNull(publisher, "No destination strategy possible for route " + getRouteId(properties));


        return publisher;
    }

    private String getDeadLetterChannel(Dictionary<String, ?> properties) {
        String dlq = (String) properties.get(ROUTE_DEADLETTER_URI);
        return dlq != null ? dlq : "log:DLQ";
    }
}
