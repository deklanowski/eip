package org.deklanowski.eip.routing.aggregator.internal.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple generic implementation of {@link ServiceRegistry}
 * TODO: this is so generic it's lovely. Move it on up ..
 * @author Declan Cox
 * @version $Id$
 * @since 15/10/2015
 */
public class ServiceRegistryImpl<T> implements ServiceRegistry<T> {


    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<String, T> registry = new ConcurrentHashMap<>();
    private final String serviceKey;

    public ServiceRegistryImpl(String serviceKey) {
        this.serviceKey = serviceKey;
    }

    @Override
    public void register(T serviceInstance,
                         Map<String, Object> properties) {

        Object type = properties.get(this.serviceKey);
        if (type != null) {
            logger.info("Registering {} of type {}", serviceInstance, type);
            registry.put((String) type, serviceInstance);
        }
    }

    @Override
    public void unregister(T serviceInstance,
                           Map<String, Object> properties) {

        Object type = properties.get(this.serviceKey);
        if (type != null) {
            logger.info("Unregistering {} of type {}", serviceInstance, type);
            registry.remove(type);
        }
    }

    @Override
    public T get(String key) {
        return registry.get(key);
    }
}
