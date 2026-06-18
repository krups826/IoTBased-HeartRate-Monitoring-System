package com.heartbeat.monitor.repository;

import com.heartbeat.monitor.model.BpmRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BpmRecordRepository extends JpaRepository<BpmRecord, Long> {
    List<BpmRecord> findByUserIdOrderByTimestampDesc(Long userId);

    @Query("SELECT b FROM BpmRecord b WHERE b.userId = :userId AND b.timestamp >= :startOfDay ORDER BY b.timestamp DESC")
    List<BpmRecord> findTodayRecordsByUserId(@Param("userId") Long userId,
            @Param("startOfDay") LocalDateTime startOfDay);
}
