package com.bigbaldy.poker;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@ActiveProfiles("dev")
@ContextConfiguration(classes = {PokerApplication.class})
public class AbstractTest {

    @Ignore
    @Test
    public void nothing() {
    }
}

