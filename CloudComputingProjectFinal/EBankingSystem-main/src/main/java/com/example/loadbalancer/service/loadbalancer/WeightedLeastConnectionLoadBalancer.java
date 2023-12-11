package com.example.loadbalancer.service.loadbalancer;

import com.example.loadbalancer.service.DockerAgent;
import com.github.dockerjava.api.model.Container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WeightedLeastConnectionLoadBalancer implements LoadBalancer {
    List<Container> containerList;
    List<Integer> weights;

    DockerAgent dockerAgent;
    public WeightedLeastConnectionLoadBalancer(List<Container> containerList, List<String> weights, DockerAgent dockerAgent){
        System.out.println("WeightedLeastConnectionLoadBalancer LoadBalancer Implementation");
        List<Integer> finalWeights;
        this.dockerAgent = dockerAgent;
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
        Container container = containerList.get(0);
        double maxScore = Integer.MIN_VALUE;
        for(int i=0;i<n;i++){
            double ioMB = dockerAgent.getIOUsage(dockerAgent,Arrays.toString(containerList.get(i).getNames()))/10000;
            System.out.println("Container: "+Arrays.toString(containerList.get(i).getNames())+" IO Usage: "+ioMB);
            double score = weights.get(i)*10/ioMB;
            if(score>maxScore){
                maxScore = score;
                container = containerList.get(i);
            }
        }
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



