package org.deklanowski.eip.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.deklanowski.eip.spi.MessagePublisher;

import java.io.Serializable;

/**
 * Message publisher processor which delegates to the an injected publisher with headers attached.
 *
 * @author deklanowski
 */
public class MessagePublisherProcessor implements Processor
{

    /**
     * The {@link MessagePublisher} instance.
     */
    private final MessagePublisher publisher;

    /**
     * Creates new publisher processor.
     * 
     * @param publisher publisher service.
     */
    public MessagePublisherProcessor(MessagePublisher publisher)
    {
        this.publisher = publisher;
    }

    @Override
    public void process(Exchange exchange) throws Exception
    {
        Message message = exchange.getIn();

        publisher.publish(message.getMandatoryBody(Serializable.class),
                exchange.getIn().getHeaders());
    }
}
