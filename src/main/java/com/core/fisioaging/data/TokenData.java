package com.core.fisioaging.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenData {

    private String token;
    private long expiresIn;
}
