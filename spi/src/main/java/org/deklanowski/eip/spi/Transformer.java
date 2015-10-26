package org.deklanowski.eip.spi;

/**
 * Generic transformer
 *
 * @author Declan Cox
 * @version $Id$
 * @since 01/10/2015
 */
public interface Transformer<I, O> {
    /**
     * Transform input type to output type
     *
     * @param input input to be transformed
     * @return transformed instance
     */
    O transform(I input);
}
