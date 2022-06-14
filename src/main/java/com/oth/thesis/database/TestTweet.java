package com.oth.thesis.database;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class TestTweet {
    @Id
    private Long id;
    private String text;
    private double score;

    public TestTweet() {
    }

    public TestTweet(Long id, String text, double score) {
        this.id = id;
        this.text = text;
        this.score = score;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
