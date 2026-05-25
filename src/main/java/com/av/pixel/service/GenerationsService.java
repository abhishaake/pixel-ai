package com.av.pixel.service;

import com.av.pixel.dao.Generations;
import com.av.pixel.dto.GenerationsDTO;
import com.av.pixel.dto.UserDTO;
import com.av.pixel.dto.VideoEffectConfigDTO;
import com.av.pixel.request.GenerateRequest;
import com.av.pixel.request.GenerationsFilterRequest;
import com.av.pixel.request.ImageActionRequest;
import com.av.pixel.request.ImagePricingRequest;
import com.av.pixel.request.ImageReportRequest;
import com.av.pixel.request.VideoEffectRequest;
import com.av.pixel.response.GenerationsFilterResponse;
import com.av.pixel.response.ImagePricingResponse;
import com.av.pixel.response.ModelConfigResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface GenerationsService {

    GenerationsFilterResponse filterImages(UserDTO userDTO, GenerationsFilterRequest imageFilterRequest, boolean includeVideoEffects);

    GenerationsDTO generate(UserDTO userDTO, GenerateRequest generateRequest, MultipartFile file);

    GenerationsDTO generateVideoEffect(UserDTO userDTO, VideoEffectRequest request, MultipartFile file);

    ImagePricingResponse getPricing (ImagePricingRequest imagePricingRequest);

    ModelConfigResponse getModelConfigs ();

    String performAction (UserDTO userDTO, ImageActionRequest imageActionRequest);

    String addView (UserDTO userDTO, ImageActionRequest imageActionRequest);

    String reportImage (UserDTO userDTO, ImageReportRequest imageReportRequest);

    void processPendingVideoEffectJobs();

    List<VideoEffectConfigDTO> getVideoEffects();

    void refreshVideoEffects();

}
