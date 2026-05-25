package com.av.pixel.service;

public interface VideoThumbnailService {

    /**
     * Extracts a JPEG thumbnail from the first second of the supplied video bytes.
     * Returns {@code null} if FFmpeg is unavailable, times out, or returns a non-zero exit code.
     * Callers must handle null gracefully (e.g. fall back to the video URL itself).
     */
    byte[] extractThumbnail(byte[] videoBytes);
}
