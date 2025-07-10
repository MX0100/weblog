package io.github.mx0100.weblog.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Gender enumeration
 * LGBTQ+ friendly gender options
 * 
 * @author mx0100
 */
@Getter
@AllArgsConstructor
public enum Gender {
    
    MALE("Male"),
    FEMALE("Female"),
    NON_BINARY("Non-binary"),
    TRANSGENDER_MALE("Transgender Male"),
    TRANSGENDER_FEMALE("Transgender Female"),
    GENDERFLUID("Genderfluid"),
    AGENDER("Agender"),
    OTHER("Other"),
    PREFER_NOT_TO_SAY("Prefer not to say");
    
    private final String description;
} 