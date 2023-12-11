package com.example.loadbalancer.service.loadbalancer;

import com.example.loadbalancer.service.loadbalancer.LoadBalancer;
import com.github.dockerjava.api.model.Container;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class IpHashLoadBalancer implements LoadBalancer {

    List<Container> containerList;
    public IpHashLoadBalancer(List<Container> containerList){
        System.out.println("IPHash LoadBalancer Implementation");
        this.containerList = containerList;
    }
    public int nextContainerPort(String ipAddress){
        int port = 9090;
        int n = containerList.size();

        String sourceIP = ipAddress;
        String destinationIP = "0:0:0:0:0:0:0:1"; //localhost
        int hashValue = IPHashFunction.hashFunction(sourceIP,destinationIP);
        Container container = containerList.get(hashValue%n);
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

class IPHashFunction {
    public static int hashFunction(String sourceIP,String destinationIP) {
        String combinedIPs = sourceIP + "-" + destinationIP;
        int hashValue;
        try {
            hashValue = calculateHash(combinedIPs);
            System.out.println("Hash value: " + hashValue);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Hash algorithm not available");
            return 0;
        }
        return hashValue;
    }

    private static int calculateHash(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
        // Take the first 4 bytes of the hash and convert to an integer
        return bytesToInt(Arrays.copyOfRange(hashBytes, 0, 4));
    }

    private static int bytesToInt(byte[] bytes) {
        int value = 0;
        for (byte b : bytes) {
            value = (value << 8) | (b & 0xFF);
        }
        return value;
    }
}

