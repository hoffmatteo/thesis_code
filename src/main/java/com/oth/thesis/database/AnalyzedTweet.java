package com.oth.thesis.database;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class AnalyzedTweet {
    @Id
    Long id;
    String text;
    double lexicon_score;
    double score2;
    double score3;

    public AnalyzedTweet(Long id, String text, double lexicon_score) {
        this.id = id;
        this.text = text;
        this.lexicon_score = lexicon_score;
    }

    public AnalyzedTweet(Long id, String text) {
        this.id = id;
        this.text = text;
        this.lexicon_score = lexicon_score;
    }

    public AnalyzedTweet() {
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

    public double getLexicon_score() {
        return lexicon_score;
    }

    public void setLexicon_score(double lexicon_score) {
        this.lexicon_score = lexicon_score;
    }

    public double getScore2() {
        return score2;
    }

    public void setScore2(double score2) {
        this.score2 = score2;
    }

    public double getScore3() {
        return score3;
    }

    public void setScore3(double score3) {
        this.score3 = score3;
    }
}
