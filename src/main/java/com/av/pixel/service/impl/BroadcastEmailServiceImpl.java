package com.av.pixel.service.impl;

import com.av.pixel.enums.SendToEnum;
import com.av.pixel.enums.EmailTemplateVariable;
import com.av.pixel.exception.Error;
import com.av.pixel.request.BroadcastEmailRequest;
import com.av.pixel.service.BroadcastEmailService;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
public class BroadcastEmailServiceImpl implements BroadcastEmailService {

    private final BroadcastEmailAsyncExecutor asyncExecutor;

    @Override
    public void queueBroadcast(BroadcastEmailRequest request) {
        validate(request);
        asyncExecutor.execute(request);
    }

    private void validate(BroadcastEmailRequest request) {
        if (request == null) {
            throw new Error("Request body is required");
        }
        if (StringUtils.isEmpty(request.getHtml())) {
            throw new Error("html is required");
        }
        if (request.getSendTo() == null) {
            throw new Error("sendTo is required");
        }
        if (SendToEnum.USER.equals(request.getSendTo()) && StringUtils.isEmpty(request.getEmail())) {
            throw new Error("email is required when sendTo is USER");
        }
        if (!CollectionUtils.isEmpty(request.getVariableNames())) {
            for (String name : request.getVariableNames()) {
                EmailTemplateVariable.fromName(name);
            }
        }
    }
}
