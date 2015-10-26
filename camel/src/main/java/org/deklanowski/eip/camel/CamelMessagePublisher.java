package org.deklanowski.eip.camel;

import org.apache.camel.ProducerTemplate;
import org.deklanowski.eip.spi.MessagePublisher;

import java.io.Serializable;
import java.util.Map;

/**
 * Handy publisher implementation which wraps a producer template. This is
 * useful for testing or simply proxying an arbitrary endpoint.
 *
 * @author deklanowski
 * @version $Id$
 */
public class CamelMessagePublisher implements MessagePublisher {

    private final ProducerTemplate template;

    public CamelMessagePublisher(ProducerTemplate template) {
        this.template = template;
    }

    @Override
    public void publish(Serializable message) {
        template.sendBody(message);
    }

    @Override
    public void publish(Serializable message,
                        Map<String, Object> headers) {
        template.sendBodyAndHeaders(message, headers);
    }

    @Override
    public void publish(Serializable message,
                        String header,
                        Object value) {

        template.sendBodyAndHeader(message, header, value);
    }
}
