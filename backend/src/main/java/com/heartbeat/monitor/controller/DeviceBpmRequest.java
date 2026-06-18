package com.heartbeat.monitor.controller;

public class DeviceBpmRequest {

    private String email;
    private int bpm;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getBpm() {
        return bpm;
    }

    public void setBpm(int bpm) {
        this.bpm = bpm;
    }
}