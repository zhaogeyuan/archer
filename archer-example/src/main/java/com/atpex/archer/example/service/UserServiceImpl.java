package com.atpex.archer.example.service;

import com.atpex.archer.annotation.Cacheable;
import com.atpex.archer.example.model.User;
import com.atpex.archer.example.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * User service
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
@Cacheable(prefix = "archer:example:user")
@Service
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;

    public UserServiceImpl(){

    }

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

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
    @ListCacheable(key = "'user:initial_page'",
            elementKey = "'user:' + #result$each.id", condition = "#pageId == 1")
    @Override
    public List<User> getUsers(int pageId, int pageSize) {
        return userRepository.queryUsers((pageId - 1), pageSize);
    }

    /**
     * 列表缓存
     *
     * @param userName
     * @return
     */
    @ListCacheable(key="#util.concat(',','list', 'by_name', #util.str(#userName))",
            elementKey = "'user:' + #result$each.id")
    @Override
    public List<User> getUsersWithName(String userName) {
        return userRepository.queryUsers(userName);
    }


    /**
     * 列表缓存
     *
     * @param userName
     * @return
     */
    @Override
    public List<User> getUsersWithNameUsingSortedSet(String userName) {
        return userRepository.queryUsers(userName);
    }

    // 对象缓存

    /**
     * 多对象缓存
     *
     * 返回结果和参数顺序一致
     *
     * @param userIds
     * @return
     */
    @MultipleCacheable(elementKey = "'user:' + #userIds$each")
    @Override
    public List<User> getUsers(@MapTo("#result$each.id") List<Long> userIds) {
        return userRepository.queryUsers(userIds);
    }

    /**
     * 多对象缓存
     *
     * 参数顺序与结果不一致
     *
     * @param userIds
     * @return
     */
    @MultipleCacheable(elementKey = "'user:' + #userIds$each", orderBy = "#result$each.userOrder()")
    @Override
    public List<User> getUsersOrderByAgeReversed(@MapTo("#result$each.id") List<Long> userIds) {
        List<User> userList = userRepository.queryUsers(userIds);
        userList.sort(Comparator.comparingInt(User::getAge).reversed());
        return userList;
    }

    /**
     * 多对象缓存
     *
     * 多参数
     *
     * @param userIds
     * @return
     */
    @MultipleCacheable(elementKey = "'user:' + #userIds$each", orderBy = "#result$each.age")
    @Override
    public List<User> getUsersWithMultipleParameters(String flag, @MapTo("#result$each.id") List<Long> userIds) {
        List<User> userList = userRepository.queryUsers(userIds);
        userList.sort(Comparator.comparingInt(User::getAge).reversed());
        return userList;
    }

    /**
     * 普通对象缓存
     *
     * @param userId
     * @return
     */
    @Cacheable(key = "'user:' + #userId")
    @Override
    public User getUser(Long userId) {
        return userRepository.queryUser(userId);
    }

    // 淘汰缓存

    /**
     * 淘汰缓存
     *
     * @param user
     */
    @CacheEvict(key = "'user:' + #user.id")
    @Override
    public void addUser(User user) {
        userRepository.addUser(user);
    }

    /**
     * 淘汰缓存
     *
     * 使用CacheContext手动淘汰缓存
     *
     * @param userId
     * @param userName
     */
    @CacheEvict(key = "'user:' + #userId")
    @Override
    public void renameUser(Long userId, String userName) {
        User user = userRepository.queryUser(userId);
        if(user != null) {
            CacheContext.evictList("example:user:list:by_name:" + user.getName());
            userRepository.renameUser(userId, userName);
        }
    }

    /**
     * 淘汰缓存
     *
     * 使用CacheContext手动淘汰缓存
     *
     * @param userId
     */
    @CacheEvict(key = "'user:' + #userId")
    @Override
    public void deleteUser(Long userId) {
        User user = userRepository.queryUser(userId);
        if(user != null) {
            CacheContext.evictList("example:user:list:by_name:" + user.getName());
            userRepository.deleteUser(userId);
        }
    }
}
