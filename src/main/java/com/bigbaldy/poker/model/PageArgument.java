package com.bigbaldy.poker.model;

import lombok.Getter;

import java.time.Instant;

public class PageArgument {
    @Getter
    private Long token;

    @Getter
    private int size;

    public PageArgument(Long token, int size) {
        this.token = token;
        this.size = size;
    }

    private PageArgument() {

    }

    public Instant getEnd() {
        return (token == null || token == 0) ? Instant.now() : Instant.ofEpochMilli(token);
    }

    public void setToken(Long token) {
        if (token != null && token > 0) {
            this.token = token - 1;
        }
    }
}
