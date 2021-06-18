package com.bigbaldy.poker.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

public class PageArgument {
    @Getter
    @Setter
    private Long token;

    @Getter
    @Setter
    private int size;

    public PageArgument(Long token, int size) {
        this.token = token;
        this.size = size;
    }

    public PageArgument(int size) {
        this.size = size;
    }

    private PageArgument() {

    }

    public Instant getEnd() {
        return (token == null || token == 0) ? Instant.now() : Instant.ofEpochMilli(token);
    }
}
