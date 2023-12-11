package com.example.loadbalancer.controller;

import com.example.loadbalancer.service.*;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


import org.springframework.web.bind.annotation.GetMapping;
        import org.springframework.web.bind.annotation.RequestMapping;
        import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/settings")
public class SettingsController {
    @Autowired
    AdminAgent adminAgent;
    private RedirectService redirectService;
    @PostMapping("/autoscaler")
    public ResponseEntity<?> autoScalerController(@RequestParam String strategy,@RequestParam String service,String username){
        User currentUser = adminAgent.addAndGetAgent(username);
        currentUser.setAutoScalerStrategy(strategy,service);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    @PostMapping("/loadbalancer")
    public ResponseEntity<?> loadBalancerController(@RequestParam String strategy,@RequestParam String service, String username,@RequestParam Optional<List<String>> weights){
        User currentUser = adminAgent.addAndGetAgent(username);
        currentUser.setLoadBalancerStrategy(strategy,service, weights.orElse(null));
        return new ResponseEntity<>(HttpStatus.OK);
    }
    @PostMapping("/start")
    public ResponseEntity<?> startServiceController(@RequestParam List<String> services, String username,@RequestParam  Optional<String> lb_strategy,@RequestParam  Optional<String> as_strategy,@RequestParam Optional<List<String>> weights){
        User currentUser = adminAgent.addAndGetAgent(username);
        List<Image> images = currentUser.getDockerAgent().listAllImages();
        String lb_strategyString = lb_strategy.orElse("weightedRoundRobin");
        String as_strategyString = as_strategy.orElse("null");
        for(String serviceNo: services){
            String imageName = "service-"+serviceNo;

            boolean imageExists = currentUser.getDockerAgent().imageAlreadyBuilt(images,imageName);
            if(!imageExists){
                System.out.println("Building image"+serviceNo);
                String imageId = currentUser.getDockerAgent().buildImage("/home/ayush/Cloud Project/LoadBalancer/EBankingSystems/Dockerfile","service-"+serviceNo); //TODO ENV VAR
                System.out.println("Built image"+serviceNo +"with ID: " + imageId);
            }
            List<Container> startedContainers = currentUser.getDockerAgent().createMultipleContainer(2,8080,imageName,username,0);
            currentUser.getServiceContainerMap().put(imageName,new ArrayList<>(startedContainers));
            currentUser.getLoadBalancerMap().put(imageName,currentUser.setLoadBalancerStrategy(lb_strategyString,serviceNo,weights.orElse(null)));
            currentUser.getAutoScalerMap().put(imageName,currentUser.setAutoScalerStrategy(as_strategyString,serviceNo));
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
    @PostMapping("/stop")
    public ResponseEntity<?> endServiceController(@RequestParam List<String> services, String username){
        User currentUser = adminAgent.addAndGetAgent(username);
        for(String serviceNo: services){
            String imageName = "service-"+serviceNo;

            List<Container> currentlyRunningServiceContainers = currentUser.getServiceContainerMap().get(imageName);

            currentUser.getDockerAgent().deleteContainers(currentlyRunningServiceContainers);
//            currentUser.getServiceContainerMap().get("service-"+serviceNo).removeAll(currentlyRunningServiceContainers);
            currentUser.getServiceContainerMap().remove(imageName);
            currentUser.getAutoScalerMap().remove(imageName);
            currentUser.getLoadBalancerMap().remove(imageName);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
    @GetMapping("/ping")
    public ResponseEntity<?> hello(){
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/startsql")
    public ResponseEntity<?> startMYSQL(@RequestParam String username){
        User currentUser = adminAgent.addAndGetAgent(username);
        List<Image> images = currentUser.getDockerAgent().listAllImages();
        System.out.println("Starting MySQL Server");
        SQLAgent startMySQLContainer = new SQLAgent(currentUser.getDockerAgent().getDockerClient());
        startMySQLContainer.run();
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
