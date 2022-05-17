package com.oth.thesis.database;

import jakarta.persistence.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Entity
public class CaseStudyTweet {
    @Id
    public long id;
    String text;
    String topic;
    @Temporal(TemporalType.TIMESTAMP)
    Date created_at;
    @Transient
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'", Locale.ENGLISH);
    //UTC TIME FORMAT!!!


    public CaseStudyTweet() {
    }

    public CaseStudyTweet(long id, String text, String created_at, String query) {
        this.id = id;
        this.text = text;
        try {
            this.created_at = formatter.parse(created_at);
        } catch (Exception e) {

        }
        this.topic = query;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Date getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Date created_at) {
        this.created_at = created_at;
    }
}
