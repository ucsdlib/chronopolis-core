package org.chronopolis.ingest.support;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Helper for testing. Just needed a constructor yay.
 * All fields are set through the Json Serializer
 *
 * Created by shake on 5/27/15.
 */
public class PageImpl<T> implements Page<T> {
    private int totalElements;
    private int totalPages;
    private int size;
    private int numberOfElements;
    private List<T> content = new ArrayList<>();
    private int number;
    private boolean last;
    private boolean first;
    private Sort sort;

    public PageImpl() {
    }

    @Override
    public int getTotalPages() {
        return totalPages;
    }

    @Override
    public long getTotalElements() {
        return totalElements;
    }

    @Override
    public int getNumber() {
        return number;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int getNumberOfElements() {
        return numberOfElements;
    }

    @Override
    public List getContent() {
        return content;
    }

    @Override
    public boolean hasContent() {
        return !content.isEmpty();
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public boolean isFirst() {
        return first;
    }

    @Override
    public boolean isLast() {
        return last;
    }

    @Override
    public boolean hasNext() {
        return last == false;
    }

    @Override
    public boolean hasPrevious() {
        return first == false;
    }

    @Override
    public Pageable nextPageable() {
        return null;
    }

    @Override
    public Pageable previousPageable() {
        return null;
    }

    @Override
    public Iterator iterator() {
        return content.iterator();
    }
}
