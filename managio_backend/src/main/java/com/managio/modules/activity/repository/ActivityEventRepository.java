package com.managio.modules.activity.repository;

import com.managio.modules.activity.domain.ActivityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Newest first; the ID breaks ties between events recorded in the same instant. */
public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long> {

    Page<ActivityEvent> findByBusinessIdOrderByCreatedAtDescIdDesc(long businessId, Pageable pageable);

    Page<ActivityEvent> findByBusinessIdAndEntityTypeOrderByCreatedAtDescIdDesc(
            long businessId, String entityType, Pageable pageable);

    Page<ActivityEvent> findByBusinessIdAndEntityTypeAndEntityIdOrderByCreatedAtDescIdDesc(
            long businessId, String entityType, long entityId, Pageable pageable);
}
