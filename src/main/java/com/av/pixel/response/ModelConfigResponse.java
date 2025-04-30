package com.av.pixel.response;

import com.av.pixel.dto.ModelConfigDTO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class ModelConfigResponse {

    List<ModelConfigDTO> models;
}
