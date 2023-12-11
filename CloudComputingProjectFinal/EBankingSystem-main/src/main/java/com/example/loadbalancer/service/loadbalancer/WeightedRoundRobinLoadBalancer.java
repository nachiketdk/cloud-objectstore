package com.example.loadbalancer.service.loadbalancer;

import com.github.dockerjava.api.model.Container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WeightedRoundRobinLoadBalancer implements LoadBalancer {
    List<Container> containerList;
    List<Integer> weights;
    private int lastAccessedContainer = -1;
    private int currentWeight = 0;
    public WeightedRoundRobinLoadBalancer(List<Container> containerList,List<String> weights){
        System.out.println("WeightedRoundRobinLoadBalancer LoadBalancer Implementation");
        List<Integer> finalWeights;
        if(weights==null){
            finalWeights = new ArrayList<>();
            List.of(containerList.size()).forEach((n)-> finalWeights.add(1)); //Default 1 weight for each container
        }
        else{
            finalWeights = new ArrayList<>();
            for(String weight: weights)
                finalWeights.add(Integer.parseInt(weight));
        }
        this.containerList = containerList;
        this.weights = finalWeights;
//        System.out.println("Weights: "+this.weights);
    }
    public int nextContainerPort(String ipAddress){
        int port = 9090;
        int n = containerList.size();
        while(weights.size()<n){
            weights.add(1);
        }
        while(weights.size()>n){
            weights.remove(weights.size()-1);
        }
        Container container;
        if(lastAccessedContainer != -1){
            if(currentWeight < weights.get(lastAccessedContainer)){
                currentWeight++;
            }
            else{
                currentWeight = 1;
                lastAccessedContainer = (lastAccessedContainer+1)%n;
            }
        }else{
            currentWeight = 1;
            lastAccessedContainer = 0;
        }
        container = containerList.get(lastAccessedContainer);
        try{
            port = container.getPorts()[0].getPublicPort();
            System.out.printf("Sending Request: Container: %s \t Port: %d \n", Arrays.toString(container.getNames()),port);
            return port;
        }
        catch (Exception e){
            System.out.printf("Could not send Request: Container: %s \t Port: %d \n", Arrays.toString(container.getNames()),port);
            System.out.println("Reason: "+e.getMessage());
        }
        return port;
    }
}



