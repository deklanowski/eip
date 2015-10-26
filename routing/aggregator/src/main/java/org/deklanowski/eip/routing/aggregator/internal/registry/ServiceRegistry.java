package org.deklanowski.eip.routing.aggregator.internal.registry;

import java.util.Map;

/**
 * Service registry abstraction.
 * TODO: generic enough to extract.
 * @author Declan Cox
 * @version $Id$
 * @since 15/10/2015
 */
public interface ServiceRegistry<T> {

    /**
     * Register a service of the given type
     * @param serviceType service interface
     * @param properties service properties
     * @param <T> type of the service
     */
    void register(T serviceType, Map<String, Object> properties);

    /**
     * unregister a service of the given type
     * @param serviceType service interface
     * @param properties service properties
     * @param <T> type of the service
     */
    void unregister(T serviceType, Map<String, Object> properties);


    /**
     * Get service instance with specified key
     * @param key registry key
     * @return serviceInstance of type T or null
     */
    T get(String key);
}
