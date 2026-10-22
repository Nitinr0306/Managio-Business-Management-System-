package com.managio.modules.tasks.repository;

import com.managio.modules.tasks.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByIdAndBusinessId(long id, long businessId);
}
