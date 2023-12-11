package com.example.loadbalancer.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Component
public class AdminAgent {
    private Map<String,User> userList;
    public AdminAgent(){
        this.userList = new HashMap<>();
    }
    public User addAndGetAgent(String username){
        if(userList!=null){
            if(userList.containsKey(username)){
                return userList.get(username);
            }
        }
        else{
            this.userList = new HashMap<>();
        }
        User newUser = new User(username);
        this.userList.put(username,newUser);
        return newUser;
    }
}
