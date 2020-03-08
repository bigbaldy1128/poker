package com.bigbaldy.poker.resource;

import com.bigbaldy.poker.exception.BaseErrorInfo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

public class PageResponseResource<T> extends ResponseResource<T> {
    private static final long serialVersionUID = -4762541243386942099L;

    @Getter
    @ApiModelProperty(value = "列表元素总数", dataType = "Long")
    private Long total;

    @Getter
    @ApiModelProperty(value = "数据位置", dataType = "Long")
    private Long position;

    public PageResponseResource() {
    }

    public PageResponseResource(int code, String message, T data, long total) {
        super(code, message, data);
        this.total = total;
    }

    public static <T> PageResponseResourceBuilder<T> builder() {
        return new PageResponseResourceBuilder<>();
    }

    public static <T> PageResponseResource<T> create(T data, Long total) {
        PageResponseResourceBuilder<T> pageResponseResourceBuilder = builder();
        pageResponseResourceBuilder.errorInfo(BaseErrorInfo.SUCCESS).data(data);
        return pageResponseResourceBuilder.total(total).build();
    }

    public static <T> PageResponseResource<T> create(T data, Long total, Long position) {
        PageResponseResourceBuilder<T> pageResponseResourceBuilder = builder();
        pageResponseResourceBuilder.errorInfo(BaseErrorInfo.SUCCESS).data(data);
        return pageResponseResourceBuilder.total(total).position(position).build();
    }

}
