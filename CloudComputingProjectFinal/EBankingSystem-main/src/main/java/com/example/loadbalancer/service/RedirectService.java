package com.example.loadbalancer.service;

import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RedirectService {
    public ResponseEntity<?> route(HttpServletRequest request,String serviceName,String newPath,User user) throws IOException {
        String body = IOUtils.toString(request.getInputStream(), Charset.forName(request.getCharacterEncoding()));
        RestTemplate restTemplate = new RestTemplate();
        String ipAddress = request.getHeader("X-Forwarded-For");

        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            // If X-Forwarded-For is not present or has an unknown value, use remote address
            ipAddress = request.getRemoteAddr();
        }

//        System.out.println("IP Address of Source: " + ipAddress);
        int newPortNumber = user.getLoadBalancerMap().get(serviceName).nextContainerPort(ipAddress);
        try {
            String redirectPath = redirectPath(newPath,newPortNumber);
            return restTemplate.exchange(redirectPath,
                    HttpMethod.valueOf(request.getMethod()),
                    new HttpEntity<>(body),
                    Object.class,
                    request.getParameterMap());
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.OK); //TODO DEBUG
//            return new ResponseEntity<>(e.getResponseBodyAsByteArray(), e.getResponseHeaders(), e.getStatusCode());
        }
    }
    public String redirectPath(String originalUrl,int newPortNumber){

        // Regex pattern to match the port number in the URL
        String regex = "^(https?://[^:/]+)(:\\d+)?(/.*)?$";

        // Create a Pattern object
        Pattern pattern = Pattern.compile(regex);

        // Create a Matcher object
        Matcher matcher = pattern.matcher(originalUrl);
        String newUrl = "";
        // Check if the URL matches the pattern
        if (matcher.matches()) {
            // Extract the protocol, host, and path
            String protocolHost = matcher.group(1);
            String path = matcher.group(3);

            // Build the new URL with the updated port number
             newUrl = protocolHost + ":" + newPortNumber + path;

//            System.out.println("Original URL: " + originalUrl);
//            System.out.println("New URL: " + newUrl);
        } else {
            System.out.println("Invalid URL format");
        }
        return newUrl;
    }
}
