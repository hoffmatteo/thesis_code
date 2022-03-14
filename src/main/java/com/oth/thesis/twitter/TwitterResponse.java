package com.oth.thesis.twitter;

import java.util.Collection;

public class TwitterResponse {
    public Collection<TwitterData> data;
    public TwitterMeta meta;
    public Collection<TwitterError> errors;
    public String title;
    public String detail;
    public String type;
    public int status;
}

