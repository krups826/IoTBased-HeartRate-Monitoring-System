package com.heartbeat.monitor.controller;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private Long id;
    private String firstName;
    private Integer age;
    private String email;
}
