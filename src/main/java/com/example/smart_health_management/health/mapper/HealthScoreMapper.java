package com.example.smart_health_management.health.mapper;

import com.example.smart_health_management.health.model.HealthScore;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;

@Mapper
public interface HealthScoreMapper {

    @Select("""
            SELECT id, user_id, score, body_status, score_date, created_at
            FROM health_score
            WHERE user_id = #{userId} AND score_date = #{scoreDate}
            LIMIT 1
            """)
    HealthScore findByUserIdAndDate(@Param("userId") Long userId, @Param("scoreDate") LocalDate scoreDate);

    @Insert("""
            INSERT INTO health_score (user_id, score, body_status, score_date)
            VALUES (#{userId}, #{score}, #{bodyStatus}, #{scoreDate})
            ON DUPLICATE KEY UPDATE score = VALUES(score), body_status = VALUES(body_status)
            """)
    int upsert(HealthScore healthScore);
}
