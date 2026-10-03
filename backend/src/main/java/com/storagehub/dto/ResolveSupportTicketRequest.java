package com.storagehub.dto;

public class ResolveSupportTicketRequest {
    private String note;

    public ResolveSupportTicketRequest() {
    }

    public ResolveSupportTicketRequest(String note) {
        this.note = note;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
