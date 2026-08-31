package com.av.pixel.service;

import com.av.pixel.dto.UserDTO;
import com.av.pixel.request.UpdateImagePrivacyRequest;
import com.av.pixel.response.ImagePrivacyResponse;

public interface ImagePrivacyService {

    ImagePrivacyResponse updateImagePrivacy (UserDTO userDTO, UpdateImagePrivacyRequest request);
}
