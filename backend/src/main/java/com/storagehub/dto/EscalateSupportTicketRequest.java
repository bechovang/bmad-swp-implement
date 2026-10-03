package com.storagehub.dto;

import jakarta.validation.constraints.NotBlank;

public class EscalateSupportTicketRequest {

    @NotBlank(message = "note must not be blank")
    private String note;

    public EscalateSupportTicketRequest() {
    }

    public EscalateSupportTicketRequest(String note) {
        this.note = note;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
