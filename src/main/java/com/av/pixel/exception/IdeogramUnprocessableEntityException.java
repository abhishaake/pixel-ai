package com.av.pixel.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class IdeogramUnprocessableEntityException extends RuntimeException{

    private String error = "Prompt provided failed safety check due to the inclusion of prohibited content.";
}
