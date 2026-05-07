package com.quran.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {
    private List<T> content;
    private Integer currentPage;
    private Integer totalPages;
    private Long totalElements;
    private Integer size;
    private Boolean hasNext;
    private Boolean hasPrevious;
}
