package com.av.pixel.service.impl;

import com.av.pixel.dao.Generations;
import com.av.pixel.dao.PromptImage;
import com.av.pixel.dao.UploadModerationLog;
import com.av.pixel.dto.ModerationResult;
import com.av.pixel.enums.ModerationDecisionEnum;
import com.av.pixel.enums.ModerationSourceEnum;
import com.av.pixel.exception.Error;
import com.av.pixel.repository.UploadModerationLogRepository;
import com.av.pixel.service.ImageModerationService;
import com.av.pixel.service.S3Service;
import com.av.pixel.service.SesEmailService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContentModerationServiceImplTest {

    private static final String USER = "P100";
    private static final String REJECTED_MESSAGE = "This image can't be used. Please upload a different photo.";

    @Mock private ImageModerationService imageModerationService;
    @Mock private UploadModerationLogRepository logRepository;
    @Mock private S3Service s3Service;
    @Mock private SesEmailService sesEmailService;
    @Mock private HttpResponse<byte[]> httpResponse;

    private ContentModerationServiceImpl service;
    private MockMultipartFile file;

    @BeforeEach
    void setUp() {
        service = new ContentModerationServiceImpl(imageModerationService, logRepository, s3Service, sesEmailService);
        file = new MockMultipartFile("character_reference_images", "ref.png", "image/png", "bytes".getBytes());
        when(imageModerationService.moderate(any())).thenReturn(ModerationResult.allowed());
        when(s3Service.downloadImage(anyString())).thenReturn(httpResponse);
        when(httpResponse.body()).thenReturn("image-bytes".getBytes());
    }

    private UploadModerationLog savedLog() {
        ArgumentCaptor<UploadModerationLog> captor = ArgumentCaptor.forClass(UploadModerationLog.class);
        verify(logRepository).save(captor.capture());
        return captor.getValue();
    }

    private static Generations imageGeneration(String... urls) {
        List<PromptImage> images = java.util.Arrays.stream(urls)
                .map(u -> new PromptImage().setUrl(u).setThumbnail(u + "_thumb"))
                .toList();
        Generations generation = new Generations()
                .setUserCode(USER)
                .setImages(images)
                .setVideoEffect(false);
        generation.setId(new ObjectId());
        return generation;
    }

    // ---------- upload path ----------

    @Test
    void skipsModerationForPrivateUploads() {
        service.assertUploadAllowed(USER, file, ModerationSourceEnum.IMAGE_GENERATION, false);

        verify(imageModerationService, never()).moderate(any());
        verify(logRepository, never()).save(any());
    }

    @Test
    void moderatesPublicUploads() {
        service.assertUploadAllowed(USER, file, ModerationSourceEnum.IMAGE_GENERATION, true);

        verify(imageModerationService).moderate(any());
    }

    @Test
    void ignoresAbsentUpload() {
        service.assertUploadAllowed(USER, null, ModerationSourceEnum.IMAGE_GENERATION, true);

        verify(imageModerationService, never()).moderate(any());
    }

    @Test
    void rejectsPublicUploadThatViolatesPolicy() {
        when(imageModerationService.moderate(any()))
                .thenReturn(ModerationResult.rejected("Explicit Nudity", 94.2d, List.of("Explicit Nudity:94.2")));

        assertThatThrownBy(() -> service.assertUploadAllowed(USER, file, ModerationSourceEnum.IMAGE_GENERATION, true))
                .isInstanceOf(Error.class)
                .hasMessage(REJECTED_MESSAGE)
                .extracting(e -> ((Error) e).getHttpStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        UploadModerationLog log = savedLog();
        assertThat(log.getUserCode()).isEqualTo(USER);
        assertThat(log.getDecision()).isEqualTo(ModerationDecisionEnum.REJECTED);
        assertThat(log.getSource()).isEqualTo(ModerationSourceEnum.IMAGE_GENERATION);
        assertThat(log.getTopLabel()).isEqualTo("Explicit Nudity");
    }

    @Test
    void recordsErrorAndAlertsWhenModerationIsUnavailable() {
        when(imageModerationService.moderate(any())).thenReturn(ModerationResult.failure(false));

        assertThatThrownBy(() -> service.assertUploadAllowed(USER, file, ModerationSourceEnum.IMAGE_GENERATION, true))
                .isInstanceOf(Error.class);

        assertThat(savedLog().getDecision()).isEqualTo(ModerationDecisionEnum.ERROR);
        verify(sesEmailService).sendErrorMail(anyString());
    }

    @Test
    void auditWriteFailureStillRejects() {
        when(imageModerationService.moderate(any()))
                .thenReturn(ModerationResult.rejected("Explicit Nudity", 94.2d, List.of()));
        when(logRepository.save(any())).thenThrow(new RuntimeException("mongo down"));

        assertThatThrownBy(() -> service.assertUploadAllowed(USER, file, ModerationSourceEnum.IMAGE_GENERATION, true))
                .isInstanceOf(Error.class)
                .hasMessage(REJECTED_MESSAGE);
    }

    // ---------- making a generation public ----------

    @Test
    void allowsCleanGenerationToBecomePublic() {
        assertThatCode(() -> service.assertGenerationAllowedPublic(USER, imageGeneration("https://s3/a.png")))
                .doesNotThrowAnyException();

        verify(imageModerationService).moderate(any());
    }

    @Test
    void rejectsGenerationWhoseImageViolatesPolicy() {
        when(imageModerationService.moderate(any()))
                .thenReturn(ModerationResult.rejected("Explicit Nudity", 91.0d, List.of("Explicit Nudity:91.0")));
        Generations generation = imageGeneration("https://s3/a.png");

        assertThatThrownBy(() -> service.assertGenerationAllowedPublic(USER, generation))
                .isInstanceOf(Error.class)
                .hasMessage(REJECTED_MESSAGE);

        UploadModerationLog log = savedLog();
        assertThat(log.getSource()).isEqualTo(ModerationSourceEnum.PRIVACY_TOGGLE);
        assertThat(log.getGenerationId()).isEqualTo(generation.getId().toString());
    }

    @Test
    void checksTheReferenceImageBecauseItIsExposedPublicly() {
        Generations generation = imageGeneration("https://s3/a.png");
        generation.setCharacterRefImageUrl("https://s3/ref.png");

        service.assertGenerationAllowedPublic(USER, generation);

        verify(s3Service).downloadImage("https://s3/ref.png");
        verify(s3Service).downloadImage("https://s3/a.png");
    }

    @Test
    void checksTheThumbnailForVideoGenerationsBecauseRekognitionCannotReadVideo() {
        Generations generation = imageGeneration("https://s3/clip.mp4");
        generation.setVideoEffect(true);

        service.assertGenerationAllowedPublic(USER, generation);

        verify(s3Service).downloadImage("https://s3/clip.mp4_thumb");
        verify(s3Service, never()).downloadImage("https://s3/clip.mp4");
    }

    @Test
    void failsClosedWhenTheImageCannotBeDownloaded() {
        when(s3Service.downloadImage(anyString())).thenThrow(new RuntimeException("s3 down"));

        assertThatThrownBy(() -> service.assertGenerationAllowedPublic(USER, imageGeneration("https://s3/a.png")))
                .isInstanceOf(Error.class)
                .hasMessage(REJECTED_MESSAGE);

        assertThat(savedLog().getDecision()).isEqualTo(ModerationDecisionEnum.ERROR);
    }

    @Test
    void stopsAtTheFirstViolationRatherThanCheckingEveryImage() {
        when(imageModerationService.moderate(any()))
                .thenReturn(ModerationResult.rejected("Explicit Nudity", 91.0d, List.of()));

        assertThatThrownBy(() -> service.assertGenerationAllowedPublic(USER,
                imageGeneration("https://s3/a.png", "https://s3/b.png", "https://s3/c.png")))
                .isInstanceOf(Error.class);

        verify(imageModerationService).moderate(any());
    }

    @Test
    void ignoresGenerationWithNothingToCheck() {
        Generations empty = new Generations().setUserCode(USER);
        empty.setId(new ObjectId());

        assertThatCode(() -> service.assertGenerationAllowedPublic(USER, empty)).doesNotThrowAnyException();
        verify(imageModerationService, never()).moderate(any());
    }

    @Test
    void videoGenerationWithoutAThumbnailFailsClosed() {
        Generations generation = new Generations()
                .setUserCode(USER)
                .setVideoEffect(true)
                .setImages(List.of(new PromptImage().setUrl("https://s3/clip.mp4")));
        generation.setId(new ObjectId());

        assertThatThrownBy(() -> service.assertGenerationAllowedPublic(USER, generation))
                .isInstanceOf(Error.class)
                .hasMessage(REJECTED_MESSAGE);

        assertThat(savedLog().getDecision()).isEqualTo(ModerationDecisionEnum.ERROR);
        verify(imageModerationService, never()).moderate(any());
    }

    @Test
    void alertIsThrottledAcrossRepeatedFailures() {
        when(imageModerationService.moderate(any())).thenReturn(ModerationResult.failure(false));

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.assertUploadAllowed(USER, file, ModerationSourceEnum.IMAGE_GENERATION, true))
                    .isInstanceOf(Error.class);
        }

        verify(sesEmailService).sendErrorMail(anyString());
        verify(logRepository, org.mockito.Mockito.times(3)).save(any());
    }

    @Test
    void failOpenResultLetsTheUploadThrough() {
        when(imageModerationService.moderate(any())).thenReturn(ModerationResult.failure(true));

        assertThatCode(() -> service.assertUploadAllowed(USER, file, ModerationSourceEnum.IMAGE_GENERATION, true))
                .doesNotThrowAnyException();

        verify(logRepository, never()).save(any());
        verify(sesEmailService).sendErrorMail(anyString());
    }

    @Test
    void emptyUploadIsIgnored() {
        MockMultipartFile emptyFile = new MockMultipartFile("f", "e.png", "image/png", new byte[0]);

        service.assertUploadAllowed(USER, emptyFile, ModerationSourceEnum.IMAGE_GENERATION, true);

        verify(imageModerationService, never()).moderate(any());
    }

    @Test
    void videoEffectUploadUsesItsOwnSource() {
        when(imageModerationService.moderate(any()))
                .thenReturn(ModerationResult.rejected("Explicit Nudity", 91.0d, List.of()));

        assertThatThrownBy(() -> service.assertUploadAllowed(USER, file, ModerationSourceEnum.VIDEO_EFFECT, true))
                .isInstanceOf(Error.class);

        assertThat(savedLog().getSource()).isEqualTo(ModerationSourceEnum.VIDEO_EFFECT);
    }

    @Test
    void recordsUploadMetadataForTheAppealTrail() {
        when(imageModerationService.moderate(any()))
                .thenReturn(ModerationResult.rejected("Explicit Nudity", 94.2d, List.of("Explicit Nudity:94.2")));

        assertThatThrownBy(() -> service.assertUploadAllowed(USER, file, ModerationSourceEnum.IMAGE_GENERATION, true))
                .isInstanceOf(Error.class);

        UploadModerationLog log = savedLog();
        assertThat(log.getContentType()).isEqualTo("image/png");
        assertThat(log.getSizeBytes()).isEqualTo((long) "bytes".getBytes().length);
        assertThat(log.getLabels()).containsExactly("Explicit Nudity:94.2");
        assertThat(log.getTopConfidence()).isEqualTo(94.2d);
    }

    @Test
    void downloadReturningNoBytesFailsClosed() {
        when(httpResponse.body()).thenReturn(null);

        assertThatThrownBy(() -> service.assertGenerationAllowedPublic(USER, imageGeneration("https://s3/a.png")))
                .isInstanceOf(Error.class);

        assertThat(savedLog().getDecision()).isEqualTo(ModerationDecisionEnum.ERROR);
        verify(imageModerationService, never()).moderate(any());
    }

    @Test
    void eachCheckedUrlIsModeratedWhenAllAreClean() {
        Generations generation = imageGeneration("https://s3/a.png", "https://s3/b.png");
        generation.setCharacterRefImageUrl("https://s3/ref.png");

        service.assertGenerationAllowedPublic(USER, generation);

        verify(imageModerationService, org.mockito.Mockito.times(3)).moderate(any());
        verify(logRepository, never()).save(any());
    }

    @Test
    void blankUrlsAreSkipped() {
        Generations generation = imageGeneration("https://s3/a.png");
        generation.setCharacterRefImageUrl("   ");

        service.assertGenerationAllowedPublic(USER, generation);

        verify(imageModerationService).moderate(any());
        verify(s3Service, never()).downloadImage(eq("   "));
    }
}
