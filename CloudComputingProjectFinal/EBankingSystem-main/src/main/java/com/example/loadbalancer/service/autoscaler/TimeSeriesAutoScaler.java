package com.example.loadbalancer.service.autoscaler;
//import org.apache.commons.math3.analysis.function.Exp;

import com.example.loadbalancer.service.DockerAgent;
import com.github.dockerjava.api.model.Container;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.*;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;

public class TimeSeriesAutoScaler implements AutoScaler{
    List<Container> containerList;
    Timer timer;

    String serviceName;

    String username;

    double alpha;

    long time;

    DockerAgent dockerAgent;
    double averageLoad =0.0d;


    public TimeSeriesAutoScaler(List<Container> containerList,DockerAgent dockerAgent,String serviceName,String username){
        System.out.println("Container Autoscaler updated to TimeSeries-Based Implementation");
        this.containerList = containerList;
        this.dockerAgent = dockerAgent;
        this.alpha = 0.2d;
        // Smoothing parameter (adjust as needed)
        this.time = System.currentTimeMillis();
        this.serviceName = serviceName;
        this.username = username;
        System.out.println("Default Aplha: "+this.alpha);
        start();
    }
    private int getQueueLength() {
        // Simulate getting the queue length from your application or system
        // In a real-world scenario, you would replace this with actual metric retrieval
        return new Random().nextInt(20);
    }

    public TimeSeriesAutoScaler(List<Container> containerList,DockerAgent dockerAgent,double alpha,String serviceName,String username){
        System.out.println("Container Autoscaler updated to TimeSeries-Based Implementation");
        System.out.println("Alpha: "+alpha);
        this.containerList = containerList;
        this.dockerAgent = dockerAgent;
        this.alpha = alpha;
        this.time = System.currentTimeMillis();
        this.serviceName = serviceName;
        this.username = username;
        start();
    }
    public void start() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new DataSnapshot2(this), 0, 1000);
        timer.scheduleAtFixedRate(new MyTimerTask2(this), 0, 10000);
    }
}

class MyTimerTask2 extends TimerTask {
    TimeSeriesAutoScaler timeSeriesAutoScaler;
    MyTimerTask2(TimeSeriesAutoScaler timeSeriesAutoScaler){
        this.timeSeriesAutoScaler = timeSeriesAutoScaler;
    }
    private double forecast(List<Integer> series) {
        // Use Simple Exponential Smoothing for time series forecasting
        double[] data = series.stream().mapToDouble(Integer::doubleValue).toArray();
        ExponentialBackOff exp = new ExponentialBackOff();
        double alpha = this.timeSeriesAutoScaler.alpha;
        double[] forecast = new double[data.length + 1];
        forecast[0] = data[0];

        for (int i = 1; i < forecast.length; i++) {
            forecast[i] = alpha * data[i - 1] + (1 - alpha) * forecast[i - 1];
        }

        return forecast[forecast.length - 1];
    }

    @Override
    public void run() {
        double averageLoad = this.timeSeriesAutoScaler.averageLoad;
        DockerAgent dockerAgent = this.timeSeriesAutoScaler.dockerAgent;
        if(averageLoad>this.timeSeriesAutoScaler.alpha){
            System.out.println("Avg Queue Length: "+(int)averageLoad*100+" Alpha: "+this.timeSeriesAutoScaler.alpha);
            System.out.println("Scaling Up");
            List<Container> newcontainers = dockerAgent.scaleUp(dockerAgent,this.timeSeriesAutoScaler.username,this.timeSeriesAutoScaler.serviceName,this.timeSeriesAutoScaler.containerList.size(),1);
            this.timeSeriesAutoScaler.containerList.addAll(newcontainers);
        }
        else if((int)averageLoad*10<this.timeSeriesAutoScaler.alpha/2){
            System.out.println("Avg Queue Length: "+(int)averageLoad*100+" Alpha: "+this.timeSeriesAutoScaler.alpha);
            System.out.println("Scaling Down");
            List<Container> deleteContainers =dockerAgent.scaleDown(dockerAgent,this.timeSeriesAutoScaler.username,this.timeSeriesAutoScaler.serviceName,this.timeSeriesAutoScaler.containerList.size(),1);
            this.timeSeriesAutoScaler.containerList.removeAll(deleteContainers);
        }
        else{
            System.out.println("Avg Queue Length: "+(int)averageLoad*100+" Alpha: "+this.timeSeriesAutoScaler.alpha);
            System.out.println("No Scaling");
        }
        long currentTime = System.currentTimeMillis();
        System.out.println("Autoscale Service for :"+ this.timeSeriesAutoScaler.username+"-container-"+this.timeSeriesAutoScaler.serviceName);
        System.out.println();
//        this.timeSeriesAutoScaler.time = currentTime;
        this.timeSeriesAutoScaler.averageLoad = 0.0d;
    }
}


class DataSnapshot2 extends TimerTask {
    TimeSeriesAutoScaler timeSeriesAutoScaler;
    DataSnapshot2(TimeSeriesAutoScaler timeSeriesAutoScaler){
        this.timeSeriesAutoScaler = timeSeriesAutoScaler;
        //TODO Debug
    }
    @Override
    public void run(){
        double totalLoad = 0.0d;
        DockerAgent dockerAgent = this.timeSeriesAutoScaler.dockerAgent;
        for(Container container: this.timeSeriesAutoScaler.containerList){
            double usage = dockerAgent.getCpuUsage(dockerAgent, Arrays.toString(container.getNames()));
//            System.out.println("USAGE: " + usage);
            totalLoad += usage;
//            System.out.println("Container: "+container.getNames()[0]+" CPU Usage: "+container.getCpuPercent());
        }
        this.timeSeriesAutoScaler.averageLoad = totalLoad/this.timeSeriesAutoScaler.containerList.size();
        // Your task logic goes here
    }
}
