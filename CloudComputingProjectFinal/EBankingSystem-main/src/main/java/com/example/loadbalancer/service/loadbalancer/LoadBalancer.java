package com.example.loadbalancer.service.loadbalancer;

import com.github.dockerjava.api.model.Container;

import org.springframework.stereotype.Service;

@Service
public interface LoadBalancer {


//    public int getNextPort();

    int nextContainerPort(String ipAddress);
}
