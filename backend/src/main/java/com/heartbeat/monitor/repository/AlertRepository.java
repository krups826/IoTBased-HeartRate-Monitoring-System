package com.heartbeat.monitor.repository;

import com.heartbeat.monitor.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByUserIdOrderByTimestampDesc(Long userId);
}
