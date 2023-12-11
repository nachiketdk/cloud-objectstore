package com.example.loadbalancer.service.loadbalancer;

import com.example.loadbalancer.service.DockerAgent;
import com.github.dockerjava.api.model.Container;

import java.util.Arrays;
import java.util.List;

public class PowerOfTwoChoicesLoadBalancer implements LoadBalancer {
    List<Container> containerList;
    DockerAgent dockerAgent;
    public PowerOfTwoChoicesLoadBalancer(List<Container> containerList, DockerAgent dockerAgent){
        System.out.println("PowerOfTwoChoicesLoadBalancer LoadBalancer Implementation");
        this.dockerAgent = dockerAgent;
        this.containerList = containerList;
    }
    public int nextContainerPort(String ipAddress){
        int port = 9090;
        int n = containerList.size();
        int randomNo1 = (int)(Math.random()*n);
//        System.out.println("Random No 1: "+randomNo1);
        int randomNo2;
        do{randomNo2 = (int)(Math.random()*n);}while(randomNo2==randomNo1);
//        System.out.println("Random No 2: "+randomNo2);

        double ioMB1 = dockerAgent.getIOUsage(dockerAgent,Arrays.toString(containerList.get(randomNo1).getNames()))/10000.0;
        double ioMB2 = dockerAgent.getIOUsage(dockerAgent,Arrays.toString(containerList.get(randomNo2).getNames()))/10000.0;

        System.out.println("Container: "+Arrays.toString(containerList.get(randomNo1).getNames())+" IO Usage: "+ioMB1);
        System.out.println("Container: "+Arrays.toString(containerList.get(randomNo2).getNames())+" IO Usage: "+ioMB2);
        Container container;
        if(ioMB1<ioMB2){
            container = containerList.get(randomNo1);
        }
        else{
            container = containerList.get(randomNo2);
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



