package com.techdesk.auth.dto.response;

public class MessageResponse {

    private String message;
    private boolean success;

    public MessageResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
    }

    public String getMessage() { return message; }
    public boolean isSuccess() { return success; }
}
