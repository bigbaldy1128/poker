package com.bigbaldy.poker.service;

import com.bigbaldy.poker.model.User;

public interface IUserService extends IService<User, Long> {
    User create();
}
