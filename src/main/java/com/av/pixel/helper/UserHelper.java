package com.av.pixel.helper;

import com.av.pixel.dto.UserDTO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@Slf4j
@AllArgsConstructor
public class UserHelper {

    Validator validator;

    private static final SecureRandom random = new SecureRandom();
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public void validateNewUserRequest(UserDTO userDTO) {
        Validator.validateNonNull(userDTO, "Request cannot be empty");
        Validator.validateNonEmpty(userDTO.getFirstName(), "Please provide valid first name");
        Validator.validateNonEmpty(userDTO.getEmail(), "Please provide valid email");
    }

    public String generatePassword() {
        int length = 6 + random.nextInt(3);
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int digit = random.nextInt(10);
            sb.append(digit);
        }

        return sb.toString();
    }

    public String getEncodedPassword() {
        String rawPassword = generatePassword();
        return encodePassword(rawPassword);
    }

    public String encodePassword(String rawPassword) {
        return encoder.encode(rawPassword);
    }
}
