package com.atpex.archer.example.service;


import com.atpex.archer.annotation.*;
import com.atpex.archer.annotation.extra.MapTo;
import com.atpex.archer.example.model.User;

import java.util.List;

/**
 * @author atpexgo.wu
 * @since 1.0.0
 */
public interface UserService {

    // 列表缓存[

    /**
     * 带条件的列表缓存
     *
     *
     * condition 用来限制缓存条件
     *
     * @param pageId
     * @param pageSize
     * @return
     */
    @CacheList(key = "'page:' + #pageId", elementKey = "#result$each.id", condition = "#pageId == 1")
    List<User> getUsers(int pageId, int pageSize);


    // 对象缓存

    /**
     * 对象缓存
     *
     * @param userId
     * @return
     */
    @Cache(key = "#userId")
    User getUser(Long userId);

    /**
     * 多对象缓存
     *
     * 返回结果和参数顺序一致
     *
     * @param userIds
     * @return
     */
    @CacheMulti(elementKey = "#userIds$each")
    List<User> getUsers(@MapTo("#result$each.id") List<Long> userIds);

    /**
     * 多对象缓存
     *
     * 参数顺序与结果不一致
     *
     * @param userIds
     * @return
     */
    @CacheMulti(elementKey = "#userIds$each", orderBy = "#result$each.userOrder()")
    List<User> getUsersOrderByAgeReversed(@MapTo("#result$each.id") List<Long> userIds);

    /**
     * 多对象缓存
     *
     * 多参数
     *
     * @param flag
     * @param userIds
     * @return
     */
    @CacheMulti(elementKey = "#userIds$each")
    List<User> getUsersWithMultipleParameters(String flag, @MapTo("#result$each.id") List<Long> userIds);

    // 淘汰缓存

    /**
     * 淘汰缓存
     *
     * @param user
     */
    @Evict(key = "#user.id")
    void addUser(User user);


    /**
     * 淘汰缓存
     *
     * @param userIds
     */
    @EvictMulti(elementKey = "#userIds$each")
    void deleteUsers(List<Long> userIds);

    /**
     * 淘汰缓存
     *
     * 使用CacheContext手动淘汰缓存
     *
     * @param userId
     * @param userName
     */
    void renameUser(Long userId, String userName);

    /**
     * 淘汰缓存
     *
     * 使用CacheContext手动淘汰缓存
     *
     * @param userId
     */
    void deleteUser(Long userId);

}
