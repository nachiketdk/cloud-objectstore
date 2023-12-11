package com.example.loadbalancer.service.autoscaler;

import com.example.loadbalancer.service.DockerAgent;
import com.github.dockerjava.api.model.Container;

import java.util.*;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;

public class ThresholdAutoScaler implements AutoScaler{
    List<Container> containerList;
    Timer timer;

    String serviceName;

    String username;

    double threshold;

    long time;


    DockerAgent dockerAgent;
    double averageLoad =0.0d;


    public ThresholdAutoScaler(List<Container> containerList,DockerAgent dockerAgent,String serviceName,String username){
        System.out.println("Container Autoscaler updated to Threshold-Based Implementation");
        this.containerList = containerList;
        this.dockerAgent = dockerAgent;
        this.threshold = 0.01d;
        this.time = System.currentTimeMillis();
        this.serviceName = serviceName;
        this.username = username;
        System.out.println("Default Threshold: "+this.threshold);
        start();
    }

    public ThresholdAutoScaler(List<Container> containerList,DockerAgent dockerAgent,double threshold,String serviceName,String username){
        System.out.println("Container Autoscaler updated to Threshold-Based Implementation");
        System.out.println("Threshold: "+threshold);
        this.containerList = containerList;
        this.dockerAgent = dockerAgent;
        this.threshold = threshold;
        this.time = System.currentTimeMillis();
        this.serviceName = serviceName;
        this.username = username;
        start();
    }
    public void start() {
//        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        timer = new Timer();
        timer.scheduleAtFixedRate(new DataSnapshot(this), 0, 1000);
        timer.scheduleAtFixedRate(new MyTimerTask(this), 0, 10000);
    }
}
//class Strategy1 implements Runnable{
//    @Override
//    public void run() {
//        // Your task logic goes here
//        System.out.println("Autoscaler Task Initiated in : " + System.currentTimeMillis());
//    }
//}

class MyTimerTask extends TimerTask {
    ThresholdAutoScaler thresholdAutoScaler;
    MyTimerTask(ThresholdAutoScaler thresholdAutoScaler){
        this.thresholdAutoScaler = thresholdAutoScaler;
    }

    @Override
    public void run() {
        double averageLoad = this.thresholdAutoScaler.averageLoad;
        DockerAgent dockerAgent = this.thresholdAutoScaler.dockerAgent;
        if(averageLoad>this.thresholdAutoScaler.threshold){
            System.out.println("Average Load: "+averageLoad+" Threshold: "+this.thresholdAutoScaler.threshold);
            System.out.println("Scaling Up");
            List<Container> newcontainers = dockerAgent.scaleUp(dockerAgent,this.thresholdAutoScaler.username,this.thresholdAutoScaler.serviceName,this.thresholdAutoScaler.containerList.size(),1);
            this.thresholdAutoScaler.containerList.addAll(newcontainers);
        }
        else if(averageLoad<this.thresholdAutoScaler.threshold/2){
            System.out.println("Average Load: "+averageLoad+" Threshold: "+this.thresholdAutoScaler.threshold);
            System.out.println("Scaling Down");
            List<Container> deleteContainers =dockerAgent.scaleDown(dockerAgent,this.thresholdAutoScaler.username,this.thresholdAutoScaler.serviceName,this.thresholdAutoScaler.containerList.size(),1);
            this.thresholdAutoScaler.containerList.removeAll(deleteContainers);
        }
        else{
            System.out.println("Average Load: "+averageLoad+" Threshold: "+this.thresholdAutoScaler.threshold);
            System.out.println("No Scaling");
        }
        long currentTime = System.currentTimeMillis();
        System.out.println("Autoscale Service for :"+ this.thresholdAutoScaler.username+"-container-"+this.thresholdAutoScaler.serviceName);
        System.out.println();
//        this.thresholdAutoScaler.time = currentTime;
        this.thresholdAutoScaler.averageLoad = 0.0d;
    }
}


class DataSnapshot extends TimerTask {
    ThresholdAutoScaler thresholdAutoScaler;
    DataSnapshot(ThresholdAutoScaler thresholdAutoScaler){
        this.thresholdAutoScaler = thresholdAutoScaler;
        List<Container> allContainers = this.thresholdAutoScaler.dockerAgent.listAllContainers(false);
//        this.thresholdAutoScaler.containerList = new ArrayList<>();
//        for(Container eachContainer: allContainers) {
//            String containerId = eachContainer.getNames()[0];
//            String[] parts = containerId.split("-");
//            if (parts[0].equals("/docker")) continue;
//
////            System.out.println("DEBUG: " + parts[0] );
////            System.out.println("DEBUG: " + parts[2]);
//            if (parts[0].equals("/" + this.thresholdAutoScaler.username)) {
//                this.thresholdAutoScaler.containerList.add(eachContainer);
//            }
//        }
        System.out.println("Currently running:"+this.thresholdAutoScaler.containerList.size()+"Containers");

    }
    @Override
    public void run(){
        double totalLoad = 0.0d;
        DockerAgent dockerAgent = this.thresholdAutoScaler.dockerAgent;
        for(Container container: this.thresholdAutoScaler.containerList){
            double usage = dockerAgent.getCpuUsage(dockerAgent, Arrays.toString(container.getNames()));
//            System.out.println("USAGE: " + usage);
            totalLoad += usage;
//            System.out.println("Container: "+container.getNames()[0]+" CPU Usage: "+container.getCpuPercent());
        }
        this.thresholdAutoScaler.averageLoad = totalLoad/this.thresholdAutoScaler.containerList.size();
        // Your task logic goes here
    }
}
