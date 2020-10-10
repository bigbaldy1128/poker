package com.bigbaldy.poker.web;

import com.bigbaldy.poker.resource.ResponseResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api("测试项目")
public class TestController {

    @ApiOperation(value = "测试方法", notes = "测试描述")
    @GetMapping("/test/{id}")
    public String test(@PathVariable("id") String id) {
        return id;
    }

    @ApiOperation(value = "测试错误", notes = "测试错误")
    @GetMapping("/test-error")
    public ResponseResource<String> testError() {
        int a = 0;
        int b = 0;
        int c = a / b;
        return ResponseResource.create("test");
    }
}
