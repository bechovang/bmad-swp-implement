package com.storagehub.dto;

import com.storagehub.entity.EscalationDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SeverityDecisionRequest {

    @NotNull(message = "decision is required")
    private EscalationDecision decision;

    @NotBlank(message = "managerNote must not be blank")
    private String managerNote;

    private Long targetUnitId;

    public SeverityDecisionRequest() {
    }

    public SeverityDecisionRequest(EscalationDecision decision, String managerNote, Long targetUnitId) {
        this.decision = decision;
        this.managerNote = managerNote;
        this.targetUnitId = targetUnitId;
    }

    public EscalationDecision getDecision() {
        return decision;
    }

    public void setDecision(EscalationDecision decision) {
        this.decision = decision;
    }

    public String getManagerNote() {
        return managerNote;
    }

    public void setManagerNote(String managerNote) {
        this.managerNote = managerNote;
    }

    public Long getTargetUnitId() {
        return targetUnitId;
    }

    public void setTargetUnitId(Long targetUnitId) {
        this.targetUnitId = targetUnitId;
    }
}
