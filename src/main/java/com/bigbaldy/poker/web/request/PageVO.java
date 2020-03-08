package com.bigbaldy.poker.web.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;

@Data
public class PageVO {
    @ApiModelProperty(value = "页索引", dataType = "Integer")
    @Min(0)
    private Integer pageIndex;

    @ApiModelProperty(value = "页大小",dataType = "Integer")
    @Min(0)
    private Integer pageSize;

    @ApiModelProperty(value = "数据位置",dataType = "Long")
    @Min(0)
    private Long position;

    public Integer getPageIndex() {
        if(pageIndex == null || pageSize == null){
            return null;
        }
        return (pageIndex - 1) * pageSize;
    }
}
