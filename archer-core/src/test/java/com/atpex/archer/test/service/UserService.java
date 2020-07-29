package com.atpex.archer.test.service;

import com.atpex.archer.annotation.Cache;
import com.atpex.archer.annotation.CacheList;
import com.atpex.archer.annotation.CacheMulti;
import com.atpex.archer.annotation.extra.MapTo;
import com.atpex.archer.test.model.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author: atpex
 * @since 1.0
 */
public class UserService {

    @Cache(key = "'user:' + #userId",
            expiration = 7, expirationTimeUnit = TimeUnit.DAYS,
            breakdownProtect = true, breakdownProtectTimeout = 300, breakdownProtectTimeUnit = TimeUnit.MILLISECONDS,
            valueSerializer = "customValueSerializer",
            keyGenerator = "customKeyGenerator",
            condition = "1 = 1",
            overwrite = false
    )
    public User getUserById(long userId){
        User user = new User();
        user.setAge(30);
        user.setId(userId);
        user.setUser("atpex");
        return user;
    }

    @CacheMulti(elementKey = "'user:' + #userIds$each",
            expiration = 7, expirationTimeUnit = TimeUnit.DAYS,
            breakdownProtect = true, breakdownProtectTimeout = 300, breakdownProtectTimeUnit = TimeUnit.MILLISECONDS,
            valueSerializer = "customValueSerializer",
            keyGenerator = "customKeyGenerator",
            condition = "1 = 1",
            overwrite = false,
            orderBy = "#result$each.age"
    )
    public List<User> getUsersByIdList(@MapTo("#result$each.id") List<Long> userIds){
        List<User> users = new ArrayList<>();
        for (Long userId : userIds) {
            users.add(getUserById(userId));
        }
        return users;
    }


    @CacheList(key = "'user:page:' + #pageId", elementKey = "'user:' + #result$each.id",
            expiration = 7, expirationTimeUnit = TimeUnit.DAYS,
            breakdownProtect = true, breakdownProtectTimeout = 300, breakdownProtectTimeUnit = TimeUnit.MILLISECONDS,
            elementValueSerializer = "customValueSerializer",
            keyGenerator = "customKeyGenerator",
            elementKeyGenerator = "customElementKeyGenerator",
            condition = "#pageId = 1",
            overwrite = false
    )
    public List<User> getPagingUsers(int pageId, int pageSize) {
        List<User> users = new ArrayList<>();
        for (Long userId : Arrays.asList(1L,2L)) {
            users.add(getUserById(userId));
        }
        return users;
    }

}
