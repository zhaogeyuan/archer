package com.atpex.archer.test.model;

import java.io.Serializable;

/**
 * @author: atpex
 * @since 1.0
 */
public class User implements Serializable {

    private long id;

    private int age;

    private String user;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", age=" + age +
                ", user='" + user + '\'' +
                '}';
    }
}
