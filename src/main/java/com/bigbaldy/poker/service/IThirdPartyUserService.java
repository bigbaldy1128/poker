package com.bigbaldy.poker.service;

import com.bigbaldy.poker.model.ThirdPartyUser;
import com.bigbaldy.poker.model.type.ThirdPartyUserType;

import java.util.Optional;

/**
 * @author wangjinzhao on 2020/3/9
 */
public interface IThirdPartyUserService extends IService<ThirdPartyUser, Long> {
    Optional<ThirdPartyUser> getByThirdPartyIdAndThirdPartyType(String thirdPartyId, ThirdPartyUserType thirdPartyUserType);
}
