package com.av.pixel.service.impl;

import com.av.pixel.dao.Generations;
import com.av.pixel.dao.ImageFlag;
import com.av.pixel.repository.ImageFlagRepository;
import com.av.pixel.request.ImageReportRequest;
import com.av.pixel.service.SesEmailService;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportTakedownServiceImplTest {

    @Mock private ImageFlagRepository imageFlagRepository;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private SesEmailService sesEmailService;

    private ReportTakedownServiceImpl service;
    private String genId;

    @BeforeEach
    void setUp() {
        service = new ReportTakedownServiceImpl(imageFlagRepository, mongoTemplate, sesEmailService, 3);
        genId = new ObjectId().toHexString();
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Generations.class)))
                .thenReturn(UpdateResult.acknowledged(1L, 1L, null));
    }

    private void storedReporters(String... userCodes) {
        List<ImageFlag> flags = Arrays.stream(userCodes)
                .map(code -> new ImageFlag().setGenId(genId).setUserCode(code))
                .toList();
        when(imageFlagRepository.findByGenId(genId)).thenReturn(flags);
    }

    private ImageReportRequest requestFor(String id) {
        ImageReportRequest request = new ImageReportRequest();
        request.setGenId(id);
        request.setReason("nudity");
        return request;
    }

    private void verifyNotHidden() {
        verify(mongoTemplate, never()).updateFirst(any(Query.class), any(Update.class), eq(Generations.class));
    }

    @Test
    void secondDistinctReporterDoesNotHide() {
        storedReporters("P1", "P2");
        service.report("P2", requestFor(genId));
        verifyNotHidden();
    }

    @Test
    void thirdDistinctReporterSetsGenerationPrivateAndAlerts() {
        storedReporters("P1", "P2", "P3");

        service.report("P3", requestFor(genId));

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(query.capture(), update.capture(), eq(Generations.class));
        assertThat(query.getValue().getQueryObject().get("_id")).isEqualTo(new ObjectId(genId));
        Document set = (Document) update.getValue().getUpdateObject().get("$set");
        assertThat(set.get("privateImage")).isEqualTo(true);
        verify(sesEmailService).sendErrorMail(anyString());
    }

    @Test
    void repeatReportFromSameUserIsIgnored() {
        when(imageFlagRepository.existsByGenIdAndUserCode(genId, "P1")).thenReturn(true);
        storedReporters("P1", "P2", "P3");

        service.report("P1", requestFor(genId));

        verify(imageFlagRepository, never()).save(any(ImageFlag.class));
        verifyNotHidden();
    }

    @Test
    void duplicateRowsFromOneUserCountOnce() {
        storedReporters("P1", "P1", "P1", "P2");
        service.report("P2", requestFor(genId));
        verifyNotHidden();
    }

    @Test
    void anonymousReportsDoNotCount() {
        storedReporters(null, null, null, "P1", "P2");
        service.report(null, requestFor(genId));
        verifyNotHidden();
    }

    @Test
    void alreadyPrivateGenerationSendsNoAlert() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Generations.class)))
                .thenReturn(UpdateResult.acknowledged(1L, 0L, null));
        storedReporters("P1", "P2", "P3");

        service.report("P3", requestFor(genId));

        verify(sesEmailService, never()).sendErrorMail(anyString());
    }

    @Test
    void hidingNeverDeletesTheGeneration() {
        storedReporters("P1", "P2", "P3");
        service.report("P3", requestFor(genId));
        verify(mongoTemplate, never()).remove(any(Query.class), eq(Generations.class));
    }

    @Test
    void mongoFailureWhileHidingDoesNotReachTheCaller() {
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Generations.class)))
                .thenThrow(new RuntimeException("mongo down"));
        storedReporters("P1", "P2", "P3");

        assertThatCode(() -> service.report("P3", requestFor(genId))).doesNotThrowAnyException();
    }

    @Test
    void malformedGenIdDoesNotReachTheCaller() {
        String bad = "not-an-object-id";
        when(imageFlagRepository.findByGenId(bad)).thenReturn(List.of(
                new ImageFlag().setGenId(bad).setUserCode("P1"),
                new ImageFlag().setGenId(bad).setUserCode("P2"),
                new ImageFlag().setGenId(bad).setUserCode("P3")));

        assertThatCode(() -> service.report("P3", requestFor(bad))).doesNotThrowAnyException();
        verifyNotHidden();
    }

    @Test
    void missingGenIdIsIgnored() {
        service.report("P1", requestFor(null));
        verify(imageFlagRepository, never()).save(any(ImageFlag.class));
    }
}
