package com.av.pixel.request;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.domain.Sort;

@Data
@Accessors(chain = true)
public class SortByRequest {
    String sortBy;
    Sort.Direction sortDir;
}
