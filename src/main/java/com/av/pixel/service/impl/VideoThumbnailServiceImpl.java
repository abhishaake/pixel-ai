package com.av.pixel.service.impl;

import com.av.pixel.service.VideoThumbnailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class VideoThumbnailServiceImpl implements VideoThumbnailService {

    private static final int FFMPEG_TIMEOUT_SECONDS = 30;
    // Seek to 1 s — safe for all GoEnhance videos (min ~3 s duration)
    private static final String SEEK_POSITION = "00:00:01";

    @Override
    public byte[] extractThumbnail(byte[] videoBytes) {
        Path tempVideo = null;
        Path tempThumb = null;
        try {
            tempVideo = Files.createTempFile("pixel_vid_", ".mp4");
            tempThumb = Files.createTempFile("pixel_thumb_", ".jpg");
            Files.write(tempVideo, videoBytes);

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y",
                    "-i", tempVideo.toAbsolutePath().toString(),
                    "-ss", SEEK_POSITION,
                    "-vframes", "1",
                    "-q:v", "2",           // JPEG quality 2 ≈ ~95% — good balance for a thumbnail
                    tempThumb.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);   // merge stderr into stdout (swallowed; we only care about exit code)

            Process process = pb.start();
            boolean finished = process.waitFor(FFMPEG_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                log.error("[VideoThumbnail] FFmpeg timed out after {}s", FFMPEG_TIMEOUT_SECONDS);
                return null;
            }
            if (process.exitValue() != 0) {
                log.error("[VideoThumbnail] FFmpeg exited with code {}", process.exitValue());
                return null;
            }

            byte[] thumbnailBytes = Files.readAllBytes(tempThumb);
            log.debug("[VideoThumbnail] extracted {} bytes", thumbnailBytes.length);
            return thumbnailBytes;

        } catch (Exception e) {
            log.error("[VideoThumbnail] extractThumbnail error", e);
            return null;
        } finally {
            deleteSilently(tempVideo);
            deleteSilently(tempThumb);
        }
    }

    private void deleteSilently(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (Exception ignored) {
                // best-effort temp-file cleanup — no action needed
            }
        }
    }
}
