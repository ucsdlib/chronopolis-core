package org.chronopolis.common.util;

/**
 * Representation of a simple filter
 *
 * Created by shake on 8/24/15.
 */
public interface Filter<E> {

    boolean add(E e);
    boolean contains(E e);

}
