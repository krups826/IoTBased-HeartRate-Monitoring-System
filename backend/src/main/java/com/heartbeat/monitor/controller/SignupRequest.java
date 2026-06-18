package com.heartbeat.monitor.controller;

import lombok.Data;

@Data
public class SignupRequest {
    private String firstName;
    private Integer age;
    private String email;
    private String password;
    private String confirmPassword;
    private String otp;
}
