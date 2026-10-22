package com.managio.platform.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.function.Function;

/** List response shape shared by every paged endpoint (docs/API.md#conventions). */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static final int MAX_PAGE_SIZE = 100;

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    /** A page of rows read with LIMIT and OFFSET, given how many rows match in total. */
    public static <T> PageResponse<T> of(List<T> rows, int page, int size, long totalItems) {
        return new PageResponse<>(rows, page, size, totalItems, (int) Math.ceil(totalItems / (double) size));
    }

    /** Builds a page request from client input, clamping the size to the allowed range. */
    public static Pageable request(Integer page, Integer size, Sort sort) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size < 1 ? 25 : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, sort);
    }
}
