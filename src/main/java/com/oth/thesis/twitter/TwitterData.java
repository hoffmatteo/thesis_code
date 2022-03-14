package com.oth.thesis.twitter;

public class TwitterData {
    public long id;
    public String text;
    public String created_at;

    @Override
    public String toString() {
        return "com.oth.thesis.twitter.TwitterData{" +
                "id=" + id +
                ", text='" + text + '\'' +
                '}';
    }
}


