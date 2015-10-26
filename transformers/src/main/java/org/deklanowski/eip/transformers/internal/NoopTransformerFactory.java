package org.deklanowski.eip.transformers.internal;


import org.deklanowski.eip.spi.Transformer;

/**
 * This is just here for blueprint purposes.
 * @author Declan Cox
 * @version $Id$
 * @since 15/10/2015
 */
public class NoopTransformerFactory {
    public Transformer<Object,Object> createNoopObjectTransformer() {
        return new NoopTransformer<>();
    }

}
