package com.av.pixel.service.impl;

import com.av.pixel.dao.Generations;
import com.av.pixel.dto.UserDTO;
import com.av.pixel.request.GenerationsFilterRequest;
import com.av.pixel.service.AdminConfigService;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Drives filterImages end to end and inspects the Query that reaches MongoTemplate. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GenerationsServiceImplFeedQueryTest {

    private static final String ME = "P100";
    private static final String OTHER = "P200";

    @Mock private MongoTemplate mongoTemplate;
    @Mock private BlockUserService blockUserService;
    @Mock private AdminConfigService adminConfigService;

    @InjectMocks private GenerationsServiceImpl service;

    private Query runAndCaptureQuery(String caller, List<String> userCodes, String privacy) {
        when(mongoTemplate.count(any(Query.class), eq(Generations.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Generations.class))).thenReturn(List.of());

        GenerationsFilterRequest request = new GenerationsFilterRequest();
        request.setUserCodes(userCodes);
        request.setPrivacy(privacy);

        service.filterImages(new UserDTO().setCode(caller), request, true);

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(query.capture(), eq(Generations.class));
        return query.getValue();
    }

    /** Finds the privateImage criterion inside the $and the service builds, or null if absent. */
    private static Object privateImageCriterion(Query query) {
        List<?> clauses = (List<?>) query.getQueryObject().get("$and");
        if (clauses == null) {
            return query.getQueryObject().get("privateImage");
        }
        return clauses.stream()
                .map(c -> ((Document) c).get("privateImage"))
                .filter(v -> v != null)
                .findFirst()
                .orElse(null);
    }

    @Test
    void strangerAskingForPrivateReachesTheQueryAsPublicOnly() {
        Query query = runAndCaptureQuery(ME, List.of(OTHER), "private");

        assertThat(privateImageCriterion(query)).isEqualTo(false);
    }

    @Test
    void ownerOnOwnProfileByDefaultAppliesNoPrivacyFilter() {
        Query query = runAndCaptureQuery(ME, List.of(ME), "default");

        assertThat(privateImageCriterion(query)).isNull();
    }
}
