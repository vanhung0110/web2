package com.hotelchain.pro.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Paginated response wrapper theo spec.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagedResponse<T> {

    private final boolean success = true;
    private final String code = "SUCCESS";
    private final String message = "Thao tác thành công";
    private final PageData<T> data;
    private final String timestamp = Instant.now().toString();
    private final String requestId = UUID.randomUUID().toString();

    public PagedResponse(Page<T> page) {
        this.data = PageData.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Getter
    @lombok.Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PageData<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;
    }
}
