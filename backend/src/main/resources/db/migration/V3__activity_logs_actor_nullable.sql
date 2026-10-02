-- =====================================================================
-- StorageHub V3 (story 1.3, decision Q1=A): activity_logs.ActorID NULL.
--
-- Why: LOGIN_FAILED must be logged for EVERY failed sign-in (AD-5). When
-- the attempted email matches no user there is no actor - ActorID becomes
-- NULL and the Reason column carries the attempted email instead. ActorID
-- keeps its FK to users(UserID), so a non-null actor is still a real user.
-- This is the ONLY schema change story 1.3 is allowed to make.
--
-- Applies on every profile (db/migration location): a fresh DB runs
-- V1 -> [V2 dev seed] -> V3; an existing V1/V2 database is ALTERed here.
-- =====================================================================

ALTER TABLE activity_logs MODIFY ActorID BIGINT NULL;
