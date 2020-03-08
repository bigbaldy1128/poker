package com.bigbaldy.poker.service.impl;

import com.bigbaldy.poker.lib.RedisClient;
import com.bigbaldy.poker.model.User;
import com.bigbaldy.poker.repository.UserRepository;
import com.bigbaldy.poker.service.AbstractDbService;
import com.bigbaldy.poker.service.IUserService;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends AbstractDbService<User, Long> implements IUserService{
    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public PagingAndSortingRepository<User, Long> getRepository() {
        return userRepository;
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
    protected Class<User> getModelClass() {
        return null;
    }

    @Override
    public User getOrCreate(String token) {
        User user = new User();
        return saveToDb(user);
    }
}
