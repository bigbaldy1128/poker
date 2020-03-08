package com.bigbaldy.poker;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

class PokerApplicationTests extends AbstractUnitTest {

    @Test
    void test() {
        List<String> list = new ArrayList<>();
        list.add("123");
        Assert.assertEquals(1, list.size());
    }
}
