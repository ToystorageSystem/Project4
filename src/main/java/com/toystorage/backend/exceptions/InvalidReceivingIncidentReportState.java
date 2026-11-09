package com.toystorage.backend.exceptions;

public class InvalidReceivingIncidentReportState extends BadRequest {
    public InvalidReceivingIncidentReportState(String message) {
        super(message);
    }
}
