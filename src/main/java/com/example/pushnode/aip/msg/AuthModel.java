package com.example.pushnode.aip.msg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthModel {
    private String deviceId;
    private String authInfo;
    private String token;
}
