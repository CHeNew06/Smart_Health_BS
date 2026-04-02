package com.example.smart_health_management.health.dto;

import java.util.List;

public class HealthAdviceResponse {

    private Double totalScore;
    private List<AdviceItem> suggestions;

    public Double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Double totalScore) {
        this.totalScore = totalScore;
    }

    public List<AdviceItem> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<AdviceItem> suggestions) {
        this.suggestions = suggestions;
    }

    public static class AdviceItem {
        private String category;
        private String title;
        private String content;

        public AdviceItem() {}

        public AdviceItem(String category, String title, String content) {
            this.category = category;
            this.title = title;
            this.content = content;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
