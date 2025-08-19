package com.chequepay.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProfileResponse {
    private String username;
    private String realname;
    private String phoneNumber;
    private String email;
    private String role;
}
