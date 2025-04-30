package com.av.pixel.service;

import java.util.List;
import java.util.TreeSet;

public interface LikeGenerationService {

    TreeSet<String> getLikedGenerationsByUserCode (String userCode, List<String> generationIds);

    String likeGeneration (String userCode, String generationId);

    String disLikeGeneration (String userCode, String generationId);
}
