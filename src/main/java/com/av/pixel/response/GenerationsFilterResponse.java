package com.av.pixel.response;

import com.av.pixel.dto.GenerationsDTO;
import com.av.pixel.response.base.PageResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class GenerationsFilterResponse extends PageResponse {

    List<GenerationsDTO> generations;

    public GenerationsFilterResponse (List<GenerationsDTO> generations, long totalCount, long page, long size) {
        super(page, size, totalCount);
        this.generations = generations;
    }

}
