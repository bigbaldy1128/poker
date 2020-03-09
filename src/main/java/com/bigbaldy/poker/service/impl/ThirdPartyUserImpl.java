package com.bigbaldy.poker.service.impl;

import com.bigbaldy.poker.lib.RedisClient;
import com.bigbaldy.poker.model.ThirdPartyUser;
import com.bigbaldy.poker.model.type.ThirdPartyUserType;
import com.bigbaldy.poker.repository.ThirdPartyUserRepository;
import com.bigbaldy.poker.service.AbstractDbService;
import com.bigbaldy.poker.service.IThirdPartyUserService;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author wangjinzhao on 2020/3/9
 */
@Service
public class ThirdPartyUserImpl extends AbstractDbService<ThirdPartyUser, Long> implements IThirdPartyUserService {
    private final ThirdPartyUserRepository thirdPartyUserRepository;

    public ThirdPartyUserImpl(ThirdPartyUserRepository thirdPartyUserRepository) {
        this.thirdPartyUserRepository = thirdPartyUserRepository;
    }

    @Override
    public Optional<ThirdPartyUser> getByThirdPartyIdAndThirdPartyType(String thirdPartyId, ThirdPartyUserType thirdPartyUserType) {
        return thirdPartyUserRepository.findByThirdPartyIdAndThirdPartyType(thirdPartyId, thirdPartyUserType);
    }

    @Override
    public PagingAndSortingRepository<ThirdPartyUser, Long> getRepository() {
        return thirdPartyUserRepository;
    }

    @Override
    protected RedisClient getRedisClient() {
        return null;
    }

    @Override
    protected long getCacheTTL() {
        return 0;
    }

    @Override
    protected Class<ThirdPartyUser> getModelClass() {
        return ThirdPartyUser.class;
    }
}
