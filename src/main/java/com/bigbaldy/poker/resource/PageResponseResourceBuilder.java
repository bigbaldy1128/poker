package com.bigbaldy.poker.resource;

public class PageResponseResourceBuilder<T> extends ResponseResourceBuilder<T> {
    private Long total;
    private Long position;

    public PageResponseResourceBuilder<T> total(Long total) {
        this.total = total;
        return this;
    }

    public PageResponseResourceBuilder<T> position(Long position) {
        this.position = position;
        return this;
    }

    @Override
    public PageResponseResource<T> build() {
        return new PageResponseResource<>(this.code, this.message, this.data, this.total);
    }
}
