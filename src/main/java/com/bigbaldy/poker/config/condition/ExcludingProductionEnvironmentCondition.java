package com.bigbaldy.poker.config.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Profiles;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author wangjinzhao on 2020/11/6
 */
public class ExcludingProductionEnvironmentCondition implements Condition {
    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        return !conditionContext.getEnvironment().acceptsProfiles(Profiles.of("production"));
    }
}
