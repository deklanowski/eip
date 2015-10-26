package org.deklanowski.eip.spi;

import java.io.Serializable;
import java.util.Map;

/**
 *
 * @author Declan Cox
 * @version $Id$
 * @since 23/10/2015
 */
public interface MessagePublisher {

    /**
     * Publish a {@link Serializable} message
     * @param message the message
     */
    void publish(Serializable message);

    /**
     * Publish a {@link Serializable} message
     * @param message the message
     * @param headers message metadata
     */
    void publish(Serializable message, Map<String, Object> headers);

    /**
     * Publish a {@link Serializable} message
     * @param message the message
     * @param header metadata key
     * @param value metadata value
     */
    void publish(Serializable message, String header, Object value);
}
