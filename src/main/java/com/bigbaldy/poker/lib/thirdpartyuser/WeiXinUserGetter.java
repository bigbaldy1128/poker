package com.bigbaldy.poker.lib.thirdpartyuser;

import com.bigbaldy.poker.model.type.ThirdPartyUserType;
import org.springframework.stereotype.Component;

/**
 * @author wangjinzhao on 2020/3/9
 */
@Component
public class WeiXinUserGetter implements IThirdPartyUserGetter {
    @Override
    public String getUserId(String token) {
        return null;
    }

    @Override
    public ThirdPartyUserType getKey() {
        return ThirdPartyUserType.WEIXIN;
    }
}
