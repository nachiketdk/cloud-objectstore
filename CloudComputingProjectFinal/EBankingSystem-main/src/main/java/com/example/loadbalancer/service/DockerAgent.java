package com.example.loadbalancer.service;//package com.loadbalancer;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

import com.github.dockerjava.api.DockerClient;

import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.core.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import com.github.dockerjava.api.command.CreateNetworkResponse;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.exception.ConflictException;
import com.github.dockerjava.api.model.*;

import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateNetworkCmd;
import com.github.dockerjava.api.command.CreateNetworkResponse;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.Network;
import java.io.File;
import java.util.List;

@Getter
@Setter
@Service
public class DockerAgent {
    DockerClient dockerClient;
    String networkName;
    public DockerAgent() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
        this.networkName = "mynetwork-net";
        CreateNetworkResponse networkResponse;

        try{
            Network existingNetwork = dockerClient.inspectNetworkCmd().withNetworkId(this.networkName).exec();
            System.out.println("Network already exists: " + existingNetwork.getId());
        }
        catch (Exception e){
            System.out.println("Network does not exist");
            networkResponse = dockerClient.createNetworkCmd().withName(this.networkName).exec();
            System.out.println("Network created: " + networkResponse.getId());
        }
    }
//    public void runContainers(List<Container> listOfContainerID){
//        for (Container container : listOfContainerID) {
//        }
//    }
    public List<Container> createMultipleContainer(int numberOfContainers,int port,String serviceName,String userName,int start){
        ExposedPort exposedPort = ExposedPort.tcp(port);
        List<Container> listOfContainers = new ArrayList<>();
        for (int i = start; numberOfContainers>0; i++) {
            if(i>=10){
                System.out.println("Maximum number of containers reached");
                break;
            }
            String containerName = userName+"-"+ serviceName +"-container-" + (i+1);
//            System.out.println("DEBUG 1");
            try {
                listOfContainers.add(createContainer(exposedPort, containerName, serviceName));
//                System.out.println("DEBUG 2");
            }catch (ConflictException e){
//                System.out.println("DEBUG 3");
                List<Container> containers = this.dockerClient.listContainersCmd().withShowAll(true).exec();
                String containerId = findContainerIdByName(containers, containerName).getId();
//                System.out.println("ID: "+containerId);
                try{
//                    System.out.println("DEBUG 4");
                    this.dockerClient.removeContainerCmd(containerId).exec();
                }catch (ConflictException e2){
                    this.dockerClient.stopContainerCmd(containerId).exec();
                    this.dockerClient.removeContainerCmd(containerId).exec();
                }
                listOfContainers.add(createContainer(exposedPort, containerName, serviceName));
            }
            numberOfContainers--;
        }
        System.out.printf("%d instances of container increased successfully: \n\n",listOfContainers.size());
//        for (Container container: listOfContainers) {
//            if(container!=null)
//                System.out.printf("Container built with ID: %s\n",container.getId());
//        }
//        System.out.println("Running the built containers one by one");
//        runContainers(listOfContainers);
        return listOfContainers;
    }
    public void buildAllImages(int n){
        List<Image> images = listAllImages();
        for(int i=1;i<=n;i++){
            String imageName = "service-"+i;
            boolean imageExists = imageAlreadyBuilt(images,imageName);
            if(!imageExists){
                System.out.println("Building image"+i);
                String filepath = "./EBankingSystems"+i+"/Dockerfile";
                String imageId = buildImage(filepath,"service-"+i); //TODO ENV VAR
                System.out.println("Built image"+i +"with ID: " + imageId);
            }
            else {
                System.out.println("Image with name: service-"+i+"already exists");
            }
        }
    }
    private Statistics getContainerStats(String containerId){
            InvocationBuilder.AsyncResultCallback<Statistics> callback = new InvocationBuilder.AsyncResultCallback<>();
//            System.out.println("DEBUG: " +containerId);
            List<Container> list = listAllContainers(false);
            String id ="";
            for(Container container: list){
//                System.out.println("DEBUG: " +Arrays.toString(container.getNames()));
                if(Arrays.toString(container.getNames()).equals(containerId)){
//                    System.out.println("DEBUG: Match Found");
                    id = container.getId();
//                    containerId = container.getId();
                }
//                else{
////                    System.out.println("DEBUG: Match Not Found");
//                }
            }
//            System.out.println("DEBUG: " + containerId.startsWith("[/"));
//            if(containerId.startsWith("[/")){
//                containerId = containerId.substring(1);
//            }
            if(id.equals("")){
//                System.out.println("DEBUG: NULL");
                return null;
            }
            this.dockerClient.statsCmd(id).exec(callback);
            Statistics stats;
            try {
                stats = callback.awaitResult();
                callback.close();
            } catch (RuntimeException | IOException e) {
                System.out.println("Unable to retrieve container stats.");
                return null;
            }
            return stats; // this may be null or invalid if the container has terminated
        }

    private Container findContainerIdByName(List<Container> containers, String containerName) {
        for (Container container : containers) {
            for (String name : container.getNames()) {
                // Check if the container name matches
                if (name.equals("/" + containerName)) {
                    return container;
                }
            }
        }
        return null; // Container not found with the specified name
    }
    private String findImageIdByName(List<Image> images, String imageName) {
        for (Image image : images) {
            if (Arrays.asList(image.getRepoTags()).contains(imageName)) {
                return image.getId();
            }
        }
        return ""; // Container not found with the specified name
    }

    public Container createContainer(ExposedPort exposedPort,String containerName,String service_tag){
        Ports portBindings = new Ports();
        portBindings.bind(exposedPort, Ports.Binding.bindPort(0));

        CreateContainerResponse container
                = this.dockerClient.createContainerCmd(service_tag +":latest")
                .withName(containerName)
                .withHostName("ayush")
                .withEnv("spring.datasource.url=jdbc:mysql://docker-mysql:3306/mysql?allowPublicKeyRetrieval=true")
                .withExposedPorts(exposedPort)
                .withPortBindings(portBindings)
                .withNetworkMode("mynetwork-net")
                .exec();
        this.dockerClient.startContainerCmd(container.getId()).exec();
        System.out.printf("Container created and started with Name: %s \\t  ID: %s\n\n",containerName,container.getId());
        return findContainerIdByName(listAllContainers(),containerName);
    }
    public void deleteContainers(List<Container> listOfContainerID){
        for (Container container : listOfContainerID) {
            if(container.getState().equals("running")){
                this.dockerClient.stopContainerCmd(container.getId()).exec();
                this.dockerClient.removeContainerCmd(container.getId()).exec(); //TODO Remove this line in the end
                System.out.println("Container stopped and removed with ID: " + container.getId());
            }
            else{
                System.out.println("Container could not be stopped. It wasn't running with ID: " + container.getId());
                try{
                    this.dockerClient.removeContainerCmd(container.getId()).exec(); //TODO Remove this line in the end
                }
                catch(Exception e){
                    System.out.println("Container could not be removed with ID: " + container.getId());
                }
            }
        }
    }

    public List<Container> listAllContainers() {
        return listAllContainers(true);
    }

    public List<Container> listAllContainers(boolean display){
        List<Container> containers = this.dockerClient.listContainersCmd().exec();
        if(display){
            System.out.println("\nList of all currently Running Containers");
            for(Container container: containers){
                System.out.println(Arrays.toString(container.getNames()));
            }
        }
        return containers;
    }
//    public List<Container> listAllContainersExited(){
//        List<Container> containers = this.dockerClient.listContainersCmd()
//                .withShowSize(true)
//                .withShowAll(true)
//                .withStatusFilter(Collections.singleton("exited")).exec();
//        System.out.println("\nList of all Exited and Running Containers");
//        for(Container container: containers){
//            System.out.println(Arrays.toString(container.getNames()));
//            System.out.println(container.getImageId());
//            System.out.println();
//        }
//        return containers;
//    }

    public List<Image> listAllImages(){
        return this.dockerClient.listImagesCmd().withShowAll(true).exec();
    }
    public boolean imageAlreadyBuilt(List<Image> images ,String service_name) {
        System.out.println(service_name);
        System.out.print("Checking if image already exists: ");
        boolean exists = images.stream().anyMatch(image -> image.getRepoTags() != null && Arrays.asList(image.getRepoTags()).contains(service_name+":latest"));
        System.out.println(exists);
        return exists;
    }
    public String buildImage(String dockerFilePath,String service_name){
        String imageId;
//        List <Image> images = listAllImages();
//        if(!imageAlreadyBuilt(images,service_name)){
        imageId = this.dockerClient.buildImageCmd()
                    .withDockerfile(new File(dockerFilePath))
                    .withPull(true)
                    .withNoCache(true)
                    .withTag(service_name+":latest")
                    .exec(new BuildImageResultCallback())
                    .awaitImageId();
//        }
//        else{
//            imageId = findImageIdByName(images,service_name);
//        }
        return imageId;
    }


// Statistics Classes of a docker file.
    public Long getMemoryUsage(DockerAgent dockerAgent,String containerName){
        Statistics stats = dockerAgent.getContainerStats(containerName);
        Long memoryUsage = stats.getMemoryStats().getUsage();
        System.out.println("Container memory stats usage: "+memoryUsage);
        return memoryUsage;
    }
    public double getCpuUsage(DockerAgent dockerAgent,String containerName){
        Statistics stats = dockerAgent.getContainerStats(containerName);
        long totalUsage = stats.getCpuStats().getCpuUsage().getTotalUsage();
        long systemCpuUsage = stats.getCpuStats().getSystemCpuUsage() ;

        // Calculate the percentage
        double cpuUsagePercentage = ((double) totalUsage / (double) systemCpuUsage) * 100.0;
//        System.out.println("Container total  cpu usage: "+totalUsage);
//        System.out.println("Container system cpu usage: "+systemCpuUsage);
//        System.out.println("Container cpu stats usage: "+cpuUsagePercentage);

        return cpuUsagePercentage;

    }
    public Long getIOUsage(DockerAgent dockerAgent,String containerName){
        Statistics stats = dockerAgent.getContainerStats(containerName);
//        Long ioUsage = stats.getMemoryStats().getUsage();
//        stats.getNetworks().forEach((k,v)-> System.out.println("Network: "+k+" "+v.getRxBytes()));
        return stats.getNetworks().get("eth0").getRxBytes();
//        System.out.println("Container memory stats usage: "+memoryUsage);
//        return memoryUsage;
    }

    public List<Container> scaleUp(DockerAgent dockerAgent, String username, String serviceName,int currentCount,int numberOfContainers) {
//        System.out.println("serviceName: "+serviceName);
//        System.out.println("username: "+username);
//        System.out.println("currentCount: "+currentCount);
        return dockerAgent.createMultipleContainer(numberOfContainers,8080,serviceName,username,currentCount);
    }

    public List<Container> scaleDown(DockerAgent dockerAgent, String username, String serviceName,int currentCount,int numberOfContainers) {
        List<Container> containers = dockerAgent.listAllContainers(false);
        List<Container> containersToBeDeleted = new ArrayList<>();
        for(int i=currentCount-1;i>2 && numberOfContainers>0;i--){
            for(Container container: containers){
                if(Arrays.toString(container.getNames()).equals(username+"-"+serviceName+"-container-"+i)){
                    containersToBeDeleted.add(container);
                }
            }
            numberOfContainers--;
        }
        dockerAgent.deleteContainers(containersToBeDeleted);
        return containersToBeDeleted;
    }

    public static void main(String[] args) {
//        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
//        List<Image> images = dockerClient.listImagesCmd().exec();
//
          DockerAgent dockerAgent = new DockerAgent();
          try{
              System.out.println(dockerAgent.getCpuUsage(dockerAgent,"root-service-1-container-1"));

//              System.out.println(dockerAgent.getIOUsage(dockerAgent,"root-service-1-container-1"));
          }
          catch (Exception e){
              System.out.println(e.getMessage());
              System.out.println(Arrays.toString(e.getStackTrace()));
          }

//        System.out.println("Container memory stats usage: "+stats.getMemoryStats().getUsage());
//        System.out.println();
//        System.out.println("Container cpu stats usage: "+stats.getCpuStats().getCpuUsage().getTotalUsage());
//        System.out.println();
//        System.out.println("Container IO usage: "+stats.getBlkioStats().toString());
//        System.out.println();


//        System.out.println("Fetching list of all running Containers");
//        listAllContainersExited(dockerClient);

//        System.out.println("Fetching list of all Containers");
//        listAllContainersExited(dockerClient);
//        System.out.println("Building image");
//        buildImage(dockerClient,"/home/ayush/Cloud Project/LoadBalancer/EBankingSystems/Dockerfile","service-1");

//        System.out.println("Deleting all containers");
//        deleteContainers(dockerClient,listAllContainersExited(dockerClient));

//        dockerClient.killContainerCmd("b93bdec6bb05d9e4fc93527e7490003d4cd246402dbc2d5f3e26fa265aa34167").exec();

//        System.out.println("Building all images");
//        dockerAgent.buildAllImages();


//        System.out.println("Starting MySQL Server");
//        StartMySQLContainer startMySQLContainer = new StartMySQLContainer(dockerClient);
//        startMySQLContainer.run();


//        System.out.println("Creating multiple Container on the built image");
//        List<Container> containers = dockerAgent.createMultipleContainer(1,8080,"service-1","root");



//        System.out.println("Fetching list of all running Containers");
//        dockerAgent.listAllContainers();


//        System.out.println("Fetching list of all running Containers");
//        listAllContainers(dockerClient);


//        InspectContainerResponse inspectContainer
//                = dockerClient.inspectContainerCmd(container.getId()).exec();


//        String imageId = dockerClient.buildImageCmd()
//                .withDockerfile(new File(""))
//                .withPull(true)
//                .withNoCache(true)
//                .withTag("service-1:latest")
//                .exec(new BuildImageResultCallback())
//                .awaitImageId();


//        List<Container> containers = dockerClient.listContainersCmd()
//                .withShowSize(true)
//                .withShowAll(true)
//                .withStatusFilter(Collections.singleton("exited")).exec();


//        String imageId = dockerClient.buildImageCmd()
//                .withDockerfile(new File("./Dockerfile"))
//                .withPull(true)
//                .withNoCache(true)
//                .withTag("api_services")
//                .exec(new BuildImageResultCallback())
//                .awaitImageId();

    }

}
