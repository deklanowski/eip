package org.deklanowski.eip.transformers.internal;


import org.deklanowski.eip.spi.Transformer;

/**
 * Generic no-op transformer.
 * @author Declan Cox
 * @version $Id$
 * @since 15/10/2015
 */
public class NoopTransformer<T> implements Transformer<T,T> {
    @Override
    public T transform(T input) {
        return input;
    }
}
