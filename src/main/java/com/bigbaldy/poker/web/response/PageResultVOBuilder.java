package com.bigbaldy.poker.web.response;

public class PageResultVOBuilder<T> extends ResultVOBuilder<T> {
    private Long total;
    private Long position;

    public PageResultVOBuilder<T> total(Long total) {
        this.total = total;
        return this;
    }

    public PageResultVOBuilder<T> position(Long position) {
        this.position = position;
        return this;
    }

    public PageResultVO<T> build() {
        return new PageResultVO<>(this.code, this.message, this.data, this.total);
    }
}
