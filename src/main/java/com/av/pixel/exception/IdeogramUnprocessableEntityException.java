package com.av.pixel.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class IdeogramUnprocessableEntityException extends RuntimeException{

    private String error = "Prompt provided failed safety check due to the inclusion of prohibited content.";

    public IdeogramUnprocessableEntityException(String error) {
        this.error = error;
    }
}
