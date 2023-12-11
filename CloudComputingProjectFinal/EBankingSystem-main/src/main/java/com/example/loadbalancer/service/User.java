package com.example.loadbalancer.service;

import com.example.loadbalancer.service.autoscaler.*;
import com.example.loadbalancer.service.loadbalancer.*;
import com.github.dockerjava.api.model.Container;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//@Component
@Getter
@Setter
public class User {
    private String username;
    private DockerAgent dockerAgent;
    private Map<String, LoadBalancer> loadBalancerMap;
    private Map<String, AutoScaler> autoScalerMap;
    private Map<String, List<Container>> serviceContainerMap;
    User(String username){
        this.username = username;
        this.dockerAgent = new DockerAgent();
        this.serviceContainerMap = new HashMap<>();
        this.loadBalancerMap = new HashMap<>();
        this.autoScalerMap = new HashMap<>();
//        this.loadBalancer = null; //TODO Set Default
//        this.autoScaler= null;//TODO Set Default
    }

    public LoadBalancer setLoadBalancerStrategy(String strategy, String serviceName, List<String> weights) {
        String serviceNameComplete = "service-"+serviceName;
        LoadBalancer loadBalancer;
        switch (strategy){
            case("weightedRoundRobin") :
                loadBalancer = new WeightedRoundRobinLoadBalancer(this.serviceContainerMap.get(serviceNameComplete),weights);
                break;
            case("random") :
                loadBalancer = new RandomLoadBalancer(this.serviceContainerMap.get(serviceNameComplete));
                break;
            case("weightedLeastConnection") :
                loadBalancer = new WeightedLeastConnectionLoadBalancer(this.serviceContainerMap.get(serviceNameComplete),weights,dockerAgent);
                break;
            case("ipHash") :
                loadBalancer = new IpHashLoadBalancer(this.serviceContainerMap.get(serviceNameComplete));
                break;
            case ("powerOfTwoChoices"):
                loadBalancer = new PowerOfTwoChoicesLoadBalancer(this.serviceContainerMap.get(serviceNameComplete),dockerAgent);
                break;
            default:
                loadBalancer = new WeightedRoundRobinLoadBalancer(this.serviceContainerMap.get(serviceNameComplete),weights);
                break;
        }
        loadBalancerMap.put(serviceNameComplete,loadBalancer);
        return loadBalancer;
    }

    public AutoScaler setAutoScalerStrategy(String strategy,String serviceName) {
        String serviceNameComplete = "service-"+serviceName;
        AutoScaler autoScaler;
        switch (strategy){
            case("threshold") :
                autoScaler = new ThresholdAutoScaler(this.serviceContainerMap.get(serviceNameComplete),dockerAgent,serviceNameComplete,username);
                break;
            case("timeseries") :
                autoScaler = new TimeSeriesAutoScaler(this.serviceContainerMap.get(serviceNameComplete),dockerAgent,serviceNameComplete,username);
                break;
            default:
                autoScaler = new NullAutoScaler(this.serviceContainerMap.get(serviceNameComplete));
                break;
            }
        autoScalerMap.put(serviceNameComplete,autoScaler);
        return autoScaler;
    }
}
