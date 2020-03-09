package com.bigbaldy.poker.repository;

import com.bigbaldy.poker.model.ThirdPartyUser;
import com.bigbaldy.poker.model.type.ThirdPartyUserType;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author wangjinzhao on 2020/3/9
 */
@Repository
public interface ThirdPartyUserRepository extends PagingAndSortingRepository<ThirdPartyUser, Long> {
    Optional<ThirdPartyUser> findByThirdPartyIdAndThirdPartyType(String thirdPartyId, ThirdPartyUserType thirdPartyUserType);
}
