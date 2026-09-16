package com.av.pixel.service.impl;

import com.av.pixel.dto.ModerationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.ModerationLabel;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImageModerationServiceImplTest {

    private static final String DEFAULT_BLOCKLIST =
            "Explicit Nudity:60";

    private static final int MAX_BYTES = 5 * 1024 * 1024;

    @Mock private RekognitionClient rekognitionClient;

    private byte[] image;

    @BeforeEach
    void setUp() throws IOException {
        image = pngOf(200, 200);
    }

    private ImageModerationServiceImpl service(boolean enabled, boolean failOpen) {
        return new ImageModerationServiceImpl(rekognitionClient, enabled, failOpen, DEFAULT_BLOCKLIST);
    }

    /** Random pixels so the PNG does not compress away — the oversize test depends on real bulk. */
    private static byte[] pngOf(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(42);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                img.setRGB(x, y, random.nextInt(0xFFFFFF));
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", out);
        return out.toByteArray();
    }

    private void respondWith(ModerationLabel... labels) {
        when(rekognitionClient.detectModerationLabels(any(DetectModerationLabelsRequest.class)))
                .thenReturn(DetectModerationLabelsResponse.builder().moderationLabels(labels).build());
    }

    private static ModerationLabel label(String name, String parent, float confidence) {
        return ModerationLabel.builder().name(name).parentName(parent).confidence(confidence).build();
    }

    private DetectModerationLabelsRequest capturedRequest() {
        ArgumentCaptor<DetectModerationLabelsRequest> captor =
                ArgumentCaptor.forClass(DetectModerationLabelsRequest.class);
        verify(rekognitionClient).detectModerationLabels(captor.capture());
        return captor.getValue();
    }

    @Test
    void rejectsExplicitNudity() {
        respondWith(label("Explicit Nudity", "Explicit", 94.2f));

        ModerationResult result = service(true, false).moderate(image);

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.isFailure()).isFalse();
        assertThat(result.getTopLabel()).isEqualTo("Explicit Nudity");
        assertThat(result.getTopConfidence()).isCloseTo(94.2d, org.assertj.core.data.Offset.offset(0.01d));
        assertThat(result.getLabels()).containsExactly("Explicit Nudity:94.2");
    }

    @Test
    void allowsKissingOnTheLips() {
        respondWith(label("Kissing on the Lips", "Non-Explicit Nudity of Intimate parts and Kissing", 99.0f));

        ModerationResult result = service(true, false).moderate(image);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.isFailure()).isFalse();
    }

    @Test
    void allowsSwimwear() {
        respondWith(label("Female Swimwear Or Underwear", "Swimwear or Underwear", 99.0f));

        ModerationResult result = service(true, false).moderate(image);

        assertThat(result.isAllowed()).isTrue();
    }

    @Test
    void rejectsExposedGenitalia() {
        respondWith(label("Exposed Female Genitalia", "Explicit Nudity", 88.0f));

        ModerationResult result = service(true, false).moderate(image);

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getTopLabel()).isEqualTo("Exposed Female Genitalia");
    }

    @Test
    void allowsWhenNoLabelsReturned() {
        respondWith();

        assertThat(service(true, false).moderate(image).isAllowed()).isTrue();
    }

    @Test
    void sendsLowestThresholdAsMinConfidence() {
        respondWith();

        service(true, false).moderate(image);

        assertThat(capturedRequest().minConfidence()).isEqualTo(60.0f);
    }

    @Test
    void skipsRekognitionEntirelyWhenDisabled() {
        ModerationResult result = service(false, false).moderate(image);

        assertThat(result.isAllowed()).isTrue();
        verify(rekognitionClient, never()).detectModerationLabels(any(DetectModerationLabelsRequest.class));
    }

    @Test
    void failsClosedWhenRekognitionThrows() {
        when(rekognitionClient.detectModerationLabels(any(DetectModerationLabelsRequest.class)))
                .thenThrow(SdkClientException.create("boom"));

        ModerationResult result = service(true, false).moderate(image);

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void failsOpenWhenConfigured() {
        when(rekognitionClient.detectModerationLabels(any(DetectModerationLabelsRequest.class)))
                .thenThrow(SdkClientException.create("boom"));

        ModerationResult result = service(true, true).moderate(image);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void treatsEmptyBytesAsAFailure() {
        ModerationResult result = service(true, false).moderate(new byte[0]);

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.isFailure()).isTrue();
        verify(rekognitionClient, never()).detectModerationLabels(any(DetectModerationLabelsRequest.class));
    }

    @Test
    void treatsUndecodableBytesAsAFailure() {
        ModerationResult result = service(true, false).moderate("not an image".getBytes());

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void downscalesOversizedImagesBeforeCallingRekognition() throws IOException {
        respondWith();
        byte[] huge = pngOf(3000, 3000);
        assertThat(huge.length).isGreaterThan(MAX_BYTES);

        service(true, false).moderate(huge);

        assertThat(capturedRequest().image().bytes().asByteArray().length).isLessThanOrEqualTo(MAX_BYTES);
    }
}
