package com.av.pixel.service;

import com.av.pixel.dao.Generations;
import com.av.pixel.dto.GenerationsDTO;
import com.av.pixel.dto.UserDTO;
import com.av.pixel.request.GenerateRequest;
import com.av.pixel.request.GenerationsFilterRequest;
import com.av.pixel.request.ImageActionRequest;
import com.av.pixel.request.ImagePricingRequest;
import com.av.pixel.response.GenerationsFilterResponse;
import com.av.pixel.response.ImagePricingResponse;
import com.av.pixel.response.ModelConfigResponse;

public interface GenerationsService {

    GenerationsFilterResponse filterImages(UserDTO userDTO, GenerationsFilterRequest imageFilterRequest);

    GenerationsDTO generate(UserDTO userDTO, GenerateRequest generateRequest);

    ImagePricingResponse getPricing (ImagePricingRequest imagePricingRequest);

    ModelConfigResponse getModelConfigs ();

    String performAction (UserDTO userDTO, ImageActionRequest imageActionRequest);

}
