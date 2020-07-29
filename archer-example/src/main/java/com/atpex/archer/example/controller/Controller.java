package com.atpex.archer.example.controller;


import example.himalaya.service.cacheable.manual.model.User;
import example.himalaya.service.cacheable.manual.service.UserService;
import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Visit: http://server:port/example/swagger-ui.html
 *
 * @author atpexgo.wu
 * @since 1.0.0
 */
@RestController
public class Controller {

    @Autowired
    private UserService userService;

    @ApiOperation(httpMethod = "GET", value = "带条件的列表缓存")
    @RequestMapping(value = "/getPaginatedUsers", method = RequestMethod.GET)
    List<User> getUsers(int pageId, int pageSize) {
        return userService.getUsers(pageId, pageSize);
    }

    @ApiOperation(httpMethod = "GET", value = "列表缓存")
    @RequestMapping(value = "/getPaginatedUsersWithName", method = RequestMethod.GET)
    List<User> getUsersWithName(String userName) {
        return userService.getUsersWithName(userName);
    }

    @ApiOperation(httpMethod = "GET", value = "多对象缓存")
    @RequestMapping(value = "/getUsers", method = RequestMethod.GET)
    List<User> getUsers() {
        List<Long> userIds = new ArrayList<>();
        userIds.add(8L);
        userIds.add(107L);
        userIds.add(203L);
        userIds.add(404L);
        userIds.add(505L);
        return userService.getUsers(userIds);
    }

    @ApiOperation(httpMethod = "GET", value = "有排序的多对象缓存")
    @RequestMapping(value = "/getUsersOrderByAgeReversed", method = RequestMethod.GET)
    List<User> getUsersOrderByAgeReversed() {
        List<Long> userIds = new ArrayList<>();
        userIds.add(8L);
        userIds.add(107L);
        userIds.add(203L);
        userIds.add(404L);
        userIds.add(505L);
        return userService.getUsersOrderByAgeReversed(userIds);
    }

    @ApiOperation(httpMethod = "GET", value = "包含无关参数的多对象缓存")
    @RequestMapping(value = "/getUsersWithMultipleParameters", method = RequestMethod.GET)
    List<User> getUsersWithMultipleParameters() {
        List<Long> userIds = new ArrayList<>();
        userIds.add(8L);
        userIds.add(107L);
        userIds.add(203L);
        userIds.add(404L);
        userIds.add(505L);
        return userService.getUsersWithMultipleParameters("testest", userIds);
    }

    @ApiOperation(httpMethod = "GET", value = "对象缓存")
    @RequestMapping(value = "/getUser", method = RequestMethod.GET)
    User getUser(Long userId) {
        return userService.getUser(userId);
    }

    @ApiOperation(httpMethod = "POST", value = "淘汰缓存")
    @RequestMapping(value = "/addUser", method = RequestMethod.POST)
    void addUser(@RequestBody User user) {
        userService.addUser(user);
    }

    @ApiOperation(httpMethod = "POST", value = "淘汰缓存, 手动淘汰")
    @RequestMapping(value = "/renameUser", method = RequestMethod.POST)
    void renameUser(Long userId, String userName) {
        userService.renameUser(userId, userName);
    }

    @ApiOperation(httpMethod = "POST", value = "淘汰缓存, 手动淘汰")
    @RequestMapping(value = "/deleteUser", method = RequestMethod.POST)
    void deleteUser(Long userId) {
        userService.deleteUser(userId);
    }

}
