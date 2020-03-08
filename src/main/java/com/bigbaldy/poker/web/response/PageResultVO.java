package com.bigbaldy.poker.web.response;

import com.bigbaldy.poker.exception.BaseErrorInfo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

public class PageResultVO<T> extends ResultVO<T> {
    private static final long serialVersionUID = -4762541243386942099L;

    @Getter
    @ApiModelProperty(value = "列表元素总数", dataType = "Long")
    private Long total;

    @Getter
    @ApiModelProperty(value = "数据位置", dataType = "Long")
    private Long position;

    public PageResultVO() {
    }

    public PageResultVO(int code, String message, T data, long total) {
        super(code, message, data);
        this.total = total;
    }

    public static <T> PageResultVOBuilder<T> builder() {
        return new PageResultVOBuilder<>();
    }

    public static <T> PageResultVO<T> create(T data, Long total) {
        PageResultVOBuilder<T> pageResultVOBuilder = builder();
        pageResultVOBuilder.errorInfo(BaseErrorInfo.SUCCESS).data(data);
        return pageResultVOBuilder.total(total).build();
    }

    public static <T> PageResultVO<T> create(T data, Long total, Long position) {
        PageResultVOBuilder<T> pageResultVOBuilder = builder();
        pageResultVOBuilder.errorInfo(BaseErrorInfo.SUCCESS).data(data);
        return pageResultVOBuilder.total(total).position(position).build();
    }

}
