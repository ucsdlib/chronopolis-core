package org.chronopolis.ingest.support;

import com.fasterxml.jackson.annotation.JsonSetter;
import org.springframework.core.convert.converter.Converter;
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
    public <S> Page<S> map(Converter<? super T, ? extends S> converter) {
        return null;
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
    public List<T> getContent() {
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

    @JsonSetter("sort")
    public void setSort(List<SortSerializer> sorts) {
        List<Sort.Order> sorders = new ArrayList<>();

        for (SortSerializer sort: sorts) {
            Sort.Direction dir = Sort.Direction.fromString(sort.direction);
            Sort.NullHandling nh = Sort.NullHandling.valueOf(sort.nullHandling);
            sorders.add(new Sort.Order(dir, sort.property, nh));
        }

        this.sort = new Sort(sorders);
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
        return !last;
    }

    @Override
    public boolean hasPrevious() {
        return !first;
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
    public Iterator<T> iterator() {
        return content.iterator();
    }

    public static class SortSerializer {
        String direction;
        String property;
        boolean ignoreCase;
        String nullHandling;
        boolean ascending;

        public SortSerializer() {
        }

        public String getDirection() {
            return direction;
        }

        public String getProperty() {
            return property;
        }

        public boolean isIgnoreCase() {
            return ignoreCase;
        }

        public String getNullHandling() {
            return nullHandling;
        }

        public boolean isAscending() {
            return ascending;
        }
    }
}
