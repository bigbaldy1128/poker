package com.bigbaldy.poker.resource;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


@Data
public class PageRequestResource {
    @ApiModelProperty(value = "页索引", dataType = "Integer")
    private Integer pageIndex;

    @ApiModelProperty(value = "页大小",dataType = "Integer")
    private Integer pageSize;

    @ApiModelProperty(value = "数据位置",dataType = "Long")
    private Long position;

    public Integer getPageIndex() {
        if(pageIndex == null || pageSize == null){
            return null;
        }
        return (pageIndex - 1) * pageSize;
    }
}
