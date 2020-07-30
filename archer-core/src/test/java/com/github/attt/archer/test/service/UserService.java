package com.github.attt.archer.test.service;

import com.github.attt.archer.annotation.Cache;
import com.github.attt.archer.annotation.CacheList;
import com.github.attt.archer.annotation.CacheMulti;
import com.github.attt.archer.annotation.extra.MapTo;
import com.github.attt.archer.test.model.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author atpex
 * @since 1.0
 */
public class UserService {

    @Cache(key = "'user:' + #userId")
    public User getUserById(long userId) {
        User user = new User();
        user.setAge(30);
        user.setId(userId);
        user.setUser("atpex");
        return user;
    }

    @CacheMulti(elementKey = "'user:' + #userIds$each")
    public List<User> getUsersByIdList(@MapTo("#result$each.id") List<Long> userIds) {
        List<User> users = new ArrayList<>();
        for (Long userId : userIds) {
            users.add(getUserById(userId));
        }
        return users;
    }


    @CacheList(key = "'user:page:' + #pageId", elementKey = "'user:' + #result$each.id")
    public List<User> getPagingUsers(int pageId, int pageSize) {
        List<User> users = new ArrayList<>();
        for (Long userId : Arrays.asList(1L, 2L)) {
            users.add(getUserById(userId));
        }
        return users;
    }

}
