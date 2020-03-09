package com.bigbaldy.poker.lib.thirdpartyuser;

import com.bigbaldy.poker.lib.IFactoryItem;
import com.bigbaldy.poker.model.type.ThirdPartyUserType;

/**
 * @author wangjinzhao on 2020/3/9
 */
public interface IThirdPartyUserGetter extends IFactoryItem<ThirdPartyUserType> {
    String getUserId(String token);
}
