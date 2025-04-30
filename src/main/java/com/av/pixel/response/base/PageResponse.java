package com.av.pixel.response.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse{
    Long totalPages;
    Long page;
    Long size;
    Long totalCount;

    public PageResponse(Long page, Long size, Long totalCount) {
        this.totalPages = calcTotalPages(totalCount, size);
        this.page = page;
        this.size = size;
        this.totalCount = totalCount;
    }

    private long calcTotalPages(long totalCount, long size) {
        if (totalCount == 0) {
            return 0;
        }
        return 1 +  ( (totalCount-1) / size);
    }
}
