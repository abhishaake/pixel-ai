package com.av.pixel.request;

import com.av.pixel.enums.SendToEnum;
import lombok.Data;

import java.util.List;

@Data
public class BroadcastEmailRequest {

    String html;

    List<String> variableNames;

    SendToEnum sendTo;

    String email;

    String subject;

    /**
     * Optional list of subject lines; one entry is chosen at random per recipient email.
     * When non-empty after trimming, takes precedence over {@link #subject}.
     */
    List<String> subjects;
}
