package com.example.wordfall.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** ページ送りのレスポンス。Spring Data の Page をそのまま返さず、形を固定する。 */
public record PageResponse<T>(List<T> content, int number, int totalPages, boolean first, boolean last) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
