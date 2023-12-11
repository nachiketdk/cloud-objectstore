package com.example.loadbalancer.controller;
import com.example.loadbalancer.service.AdminAgent;
import com.example.loadbalancer.service.RedirectService;

import com.example.loadbalancer.service.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/service4")
public class Service4Controller {
    @Autowired
    private RedirectService redirectService;
    @Autowired
    AdminAgent adminAgent;

    @RequestMapping(value = "/**")
    public ResponseEntity<?> route(HttpServletRequest request, @RequestParam String company) throws IOException {
        Map<String,User> map = adminAgent.getUserList();
        User currentUser;
        if(map.containsKey(company)){
            currentUser = map.get(company);
        }
        else{
            return new ResponseEntity<>("The company name is not registered with us yet.",HttpStatus.NOT_FOUND);
        }
        String firstUrl = "http://localhost:8080";
        String fullPath = request.getRequestURI();
        System.out.println("fullPath: "+fullPath);
        String dynamicPath = fullPath.replaceFirst("/service4/", "/");
        String newPath = firstUrl + dynamicPath;
        return redirectService.route(request,"service-4",newPath,currentUser);
    }
    @GetMapping("/ping")
    public ResponseEntity<?> hello(){
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
