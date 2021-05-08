package com.bigbaldy.poker;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@ActiveProfiles("local")
@ContextConfiguration(classes = {PokerApplication.class})
public class AbstractTest {
}

