package com.storagehub.service;

import com.storagehub.entity.Action;
import com.storagehub.entity.ActivityLog;
import com.storagehub.entity.EntityType;
import com.storagehub.repository.ActivityLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Pure Mockito tests of LogService (no DB): column mapping of append(), the
 * reason registry contract (reason-required actions are refused with a blank
 * reason and the repository is never touched) and the NOT NULL story for
 * optional reasons ("" is stored, not null).
 */
@ExtendWith(MockitoExtension.class)
class LogServiceTests {

    @Mock
    private ActivityLogRepository activityLogRepository;

    @InjectMocks
    private LogService logService;

    @Captor
    private ArgumentCaptor<ActivityLog> logCaptor;

    @Test
    void appendMapsEveryArgumentIntoTheSavedRow() {
        logService.append(7L, EntityType.UNIT, 3L, Action.STATUS_CHANGE, "AVAILABLE", "MAINTENANCE", null);

        verify(activityLogRepository).save(logCaptor.capture());
        ActivityLog saved = logCaptor.getValue();
        assertThat(saved.getActorId()).isEqualTo(7L);
        assertThat(saved.getEntityType()).isEqualTo(EntityType.UNIT);
        assertThat(saved.getEntityId()).isEqualTo(3L);
        assertThat(saved.getAction()).isEqualTo(Action.STATUS_CHANGE);
        assertThat(saved.getFromValue()).isEqualTo("AVAILABLE");
        assertThat(saved.getToValue()).isEqualTo("MAINTENANCE");
    }

    @Test
    void optionalReasonNullIsStoredAsEmptyStringForTheNotNullColumn() {
        logService.append(7L, EntityType.UNIT, 3L, Action.STATUS_CHANGE, "AVAILABLE", "MAINTENANCE", null);

        verify(activityLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getReason()).isEmpty();
    }

    @Test
    void providedReasonIsStoredVerbatim() {
        logService.append(1L, EntityType.POLICY, 3L, Action.STATUS_CHANGE, null, "ACTIVE",
                "policy v3 activated for the demo set");

        verify(activityLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getReason()).isEqualTo("policy v3 activated for the demo set");
    }

    @Test
    void reasonRequiredActionWithNullReasonIsRefusedBeforeInsert() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> logService.append(7L, EntityType.UNIT, 3L,
                        Action.FIX_STATUS, "MAINTENANCE", "RETIRED", null))
                .withMessageContaining("FIX_STATUS");
        verifyNoInteractions(activityLogRepository);
    }

    @Test
    void reasonRequiredActionWithBlankReasonIsRefusedBeforeInsert() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> logService.append(7L, EntityType.CONTRACT, 3L,
                        Action.WAIVER, null, null, "   "))
                .withMessageContaining("WAIVER");
        verifyNoInteractions(activityLogRepository);
    }

    @Test
    void everyReasonRequiredActionIsRefusedWhenReasonIsMissing() {
        for (Action action : Action.values()) {
            if (!action.requiresReason()) {
                continue;
            }
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> logService.append(1L, EntityType.USER, 1L, action, null, null, null))
                    .withMessageContaining(action.name());
        }
        verifyNoInteractions(activityLogRepository);
    }

    @Test
    void everyReasonOptionalActionIsAcceptedWithoutReason() {
        long optionalActions = java.util.Arrays.stream(Action.values())
                .filter(action -> !action.requiresReason())
                .count();
        for (Action action : Action.values()) {
            if (!action.requiresReason()) {
                logService.append(1L, EntityType.USER, 1L, action, null, null, null);
            }
        }
        verify(activityLogRepository, org.mockito.Mockito.times((int) optionalActions))
                .save(any(ActivityLog.class));
    }

    @Test
    void textLongerThanTheVarchar255ColumnsIsRefusedBeforeInsert() {
        String tooLong = "x".repeat(256);
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> logService.append(7L, EntityType.UNIT, 3L,
                        Action.STATUS_CHANGE, "AVAILABLE", tooLong, null))
                .withMessageContaining("255");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> logService.append(7L, EntityType.CONTRACT, 3L,
                        Action.WAIVER, null, null, tooLong))
                .withMessageContaining("255");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> logService.append(7L, EntityType.UNIT, 3L,
                        Action.STATUS_CHANGE, tooLong, "RETIRED", null))
                .withMessageContaining("255");
        verifyNoInteractions(activityLogRepository);
    }

    @Test
    void textAtExactly255CharactersIsAccepted() {
        String max = "x".repeat(255);
        logService.append(7L, EntityType.UNIT, 3L, Action.STATUS_CHANGE, max, max, max);
        verify(activityLogRepository).save(any(ActivityLog.class));
    }
}
