package com.example.loadbalancer.service.autoscaler;

import com.github.dockerjava.api.model.Container;

import java.util.List;

public class NullAutoScaler implements AutoScaler {
    public NullAutoScaler(List<Container> containers) {
        System.out.println("No AutoScaler Selected by Default");

    }

    @Override
    public void start() {

    }
}
