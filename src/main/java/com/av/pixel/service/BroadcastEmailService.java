package com.av.pixel.service;

import com.av.pixel.request.BroadcastEmailRequest;

public interface BroadcastEmailService {

    void queueBroadcast(BroadcastEmailRequest request);
}
