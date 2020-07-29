package com.atpex.archer.example.service;


import com.atpex.archer.example.model.User;

import java.util.List;

/**
 * @author atpexgo.wu
 * @since 1.0.0
 */
public interface UserService {

    List<User> getUsers(int pageId, int pageSize);

    List<User> getUsersWithName(String userName);

    List<User> getUsersWithNameUsingSortedSet(String userName);

    List<User> getUsers(List<Long> userIds);

    List<User> getUsersOrderByAgeReversed(List<Long> userIds);

    List<User> getUsersWithMultipleParameters(String flag,List<Long> userIds);

    User getUser(Long userId);

    void addUser(User user);

    void renameUser(Long userId, String userName);

    void deleteUser(Long userId);

}
