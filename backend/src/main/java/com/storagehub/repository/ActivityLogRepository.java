package com.storagehub.repository;

import com.storagehub.entity.ActivityLog;
import org.springframework.data.repository.Repository;

/**
 * Append-only data access to activity_logs (NFR-6): extends the base marker
 * {@link Repository} - not JpaRepository/PagingAndSortingRepository - and
 * declares exactly one method, save. No update, no delete, no find exists on
 * this type, so the audit trail cannot be rewritten or read around LogService
 * (AD-6). Read paths for the Activity Log screen are added with a dedicated
 * read-side when their story lands.
 */
public interface ActivityLogRepository extends Repository<ActivityLog, Long> {

    <S extends ActivityLog> S save(S entity);
}
