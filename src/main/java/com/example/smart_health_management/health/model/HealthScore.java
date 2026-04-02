package com.example.smart_health_management.health.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class HealthScore {
    private Long id;
    private Long userId;
    private Integer score;
    private String bodyStatus;
    private LocalDate scoreDate;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getBodyStatus() { return bodyStatus; }
    public void setBodyStatus(String bodyStatus) { this.bodyStatus = bodyStatus; }
    public LocalDate getScoreDate() { return scoreDate; }
    public void setScoreDate(LocalDate scoreDate) { this.scoreDate = scoreDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
