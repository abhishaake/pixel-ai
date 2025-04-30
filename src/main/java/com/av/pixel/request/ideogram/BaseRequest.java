package com.av.pixel.request.ideogram;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BaseRequest {

    @JsonProperty("image_request")
    private ImageRequest imageRequest;
}
