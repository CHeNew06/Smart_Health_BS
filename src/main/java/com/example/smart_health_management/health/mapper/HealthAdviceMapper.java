package com.example.smart_health_management.health.mapper;

import com.example.smart_health_management.health.model.HealthAdvice;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface HealthAdviceMapper {

    @Delete("""
            DELETE FROM health_advice
            WHERE user_id = #{userId} AND advice_date = #{adviceDate}
            """)
    int deleteByUserIdAndDate(@Param("userId") Long userId, @Param("adviceDate") LocalDate adviceDate);

    @Insert("""
            INSERT INTO health_advice (user_id, category, title, content, advice_date)
            VALUES (#{userId}, #{category}, #{title}, #{content}, #{adviceDate})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(HealthAdvice advice);

    @Select("""
            SELECT id, user_id, category, title, content, tags, advice_date, created_at
            FROM health_advice
            WHERE user_id = #{userId} AND advice_date = #{adviceDate}
            ORDER BY category
            """)
    List<HealthAdvice> findByUserIdAndDate(@Param("userId") Long userId, @Param("adviceDate") LocalDate adviceDate);
}
