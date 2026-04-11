package com.av.pixel.enums;

import com.av.pixel.dao.User;
import com.av.pixel.exception.Error;
import io.micrometer.common.util.StringUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@Getter
@RequiredArgsConstructor
public enum EmailTemplateVariable {

    USER_NAME(user -> {
        if (user == null) {
            return "";
        }
        String first = emptyIfNull(user.getFirstName()).trim();
        if (first.isEmpty()) {
            return "";
        }
        return first.split("\\s+")[0];
    }),
    USER_EMAIL(user -> user == null || StringUtils.isEmpty(user.getEmail()) ? "" : user.getEmail()),
    USER_CODE(user -> user == null || StringUtils.isEmpty(user.getCode()) ? "" : user.getCode()),
    USER_FIRST_NAME(user -> user == null ? "" : emptyIfNull(user.getFirstName())),
    USER_LAST_NAME(user -> user == null ? "" : emptyIfNull(user.getLastName())),
    USER_PHONE(user -> user == null ? "" : emptyIfNull(user.getPhone())),
    /**
     * Placeholder replaced per outbound email with a fresh UUID in the broadcast sender (not from {@link #resolve}).
     */
    EMAIL_IDENTIFIER(user -> "");

    private final Function<User, String> resolver;

    public String resolve(User user) {
        return resolver.apply(user);
    }

    public static EmailTemplateVariable fromName(String name) {
        if (name == null) {
            throw new Error("Template variable name cannot be empty");
        }
        String trimmed = name.trim();
        if (StringUtils.isEmpty(trimmed)) {
            throw new Error("Template variable name cannot be empty");
        }
        try {
            return EmailTemplateVariable.valueOf(trimmed);
        } catch (IllegalArgumentException e) {
            throw new Error("Unsupported template variable: " + name);
        }
    }

    private static String emptyIfNull(String s) {
        return s == null ? "" : s;
    }
}
