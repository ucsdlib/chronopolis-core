package org.chronopolis.ingest;

import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;

/**
 * Test class for testing paging in templates
 *
 * Created by shake on 1/5/16.
 */
public class PageWrapper<T> {
    private final Logger log = LoggerFactory.getLogger(PageWrapper.class);

    private static final int MAX_SIZE = 5;

    private List<Integer> pages;
    private boolean firstParam = true;

    private String url;
    private final Integer next;
    private final Integer last;
    private final Integer current;
    private final Integer previous;

    public PageWrapper(Page<T> page, String url, Multimap<String, String> params) {
        this.url = url;
        this.pages = new ArrayList<>();
        this.current = page.getNumber();

        this.previous = (current == 0) ? 0 : current - 1;
        this.next = (current == page.getTotalPages() - 1) ? current : current + 1;
        this.last = page.getTotalPages() - 1;

        createPages(page);
        params.forEach(this::append);
    }

    private void append(String key, String val) {
        if (key != null) {
            String appender = "&";
            if (firstParam) {
                firstParam = false;
                appender = "?";
            }

            url += String.format("%s%s=%s", appender, key, val);
        }
    }

    private void createPages(Page<T> page) {
        int total = page.getTotalPages();

        int start, end;
        if (total < MAX_SIZE) {
            start = 0;
            end = total;
        } else {
            start = page.getNumber() - MAX_SIZE/2;

            // underflow and overflow handling
            if (start <= 0) {
                start = 0;
            } else if (start + MAX_SIZE > total ) {
                start = total - MAX_SIZE;
            }

            end = MAX_SIZE;
        }

        for (int i = 0; i < end; i++) {
            pages.add(start + i);
        }
    }

    public List<Integer> getPages() {
        return pages;
    }

    public String getUrl() {
        return url;
    }

    public Integer getCurrent() {
        return current;
    }

    public Integer getNext() {
        return next;
    }

    public Integer getPrevious() {
        return previous;
    }

    public Integer getLast() {
        return last;
    }

    public Integer getFirst() {
        return 0;
    }
}
