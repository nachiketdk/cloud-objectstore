package com.example.loadbalancer.service;

import com.github.dockerjava.api.DockerClient;
        import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.CreateNetworkResponse;
import com.github.dockerjava.api.exception.ConflictException;
import com.github.dockerjava.api.model.*;

import java.io.File;
import java.util.List;

public class SQLAgent {
    public final DockerClient dockerClient;
    public SQLAgent(DockerClient dockerClient){
        this.dockerClient = dockerClient;
    }

    private static String findContainerIdByName(List<Container> containers, String containerName) {
        for (Container container : containers) {
            for (String name : container.getNames()) {
                // Check if the container name matches
                if (name.equals("/" + containerName)) {
                    return container.getId();
                }
            }
        }
        return null; // Container not found with the specified name
    }
    public void run() {
        try{
            String networkName = "mynetwork-net";
            CreateNetworkResponse networkResponse = null;

            try{
                Network existingNetwork = dockerClient.inspectNetworkCmd().withNetworkId(networkName).exec();
                System.out.println("Network already exists: " + existingNetwork.getId());
            }
            catch (Exception e){
                System.out.println("Network does not exist");
                networkResponse = dockerClient.createNetworkCmd().withName(networkName).exec();
                System.out.println("Network created: " + networkResponse.getId());

            }

            // Define environment variables
            String[] env = {"MYSQL_ROOT_PASSWORD=password", "MYSQL_DATABASE=mysql", "MYSQL_PASSWORD=password"};
            Ports portBindings = new Ports();
            ExposedPort exposedPort = ExposedPort.tcp(3306);
            portBindings.bind(exposedPort, Ports.Binding.bindPort(3307));

            // Define port bindings
//            Ports.Binding hostBinding = Ports.Binding.bindPort(3307);
//            Ports portBindings = new Ports(Arrays.asList(new PortBinding(hostBinding, containerBinding)));

            // Define volumes
            Volume volume = new Volume("/var/lib/mysql");

            // Create host configuration
//            HostConfig hostConfig = HostConfig.newHostConfig()
//                    .withPortBindings(portBindings);
//                    .withNetworkMode("springapimysql-net") // Assuming the network exists
//                    .withBinds(new HostConfig.Bind("./docker/mysql/data", volume));

            // Create container
            File f = new File("user_data.txt");

            // Get the absolute path of file f
            String absolute = f.getAbsolutePath();

            String newAbsolute = absolute.substring(0,absolute.length()-13);
            String containerName = "docker-mysql";
            CreateContainerResponse container;
            String filePath = newAbsolute + "docker/mysql/data";

            try {
                container = this.dockerClient.createContainerCmd("mariadb:11.1.3")
                        .withEnv(env)
                        .withName(containerName)
                        .withExposedPorts(exposedPort)
                        .withPortBindings(portBindings)
                        .withVolumes(volume)
                        .withBinds(new Bind(filePath, volume))
                        .withNetworkMode("mynetwork-net")
                        .exec();
            }catch (ConflictException e){
                List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
                String containerId = findContainerIdByName(containers, containerName);
                System.out.println(containerId);
                try{
                    dockerClient.stopContainerCmd(containerId).exec();
                    dockerClient.removeContainerCmd(containerId).exec();
                }catch(Exception e2){
                    dockerClient.removeContainerCmd(containerId).exec();
                }
                container = this.dockerClient.createContainerCmd("mariadb:11.1.3")
                        .withEnv(env)
                        .withName("docker-mysql")
                        .withExposedPorts(exposedPort)
                        .withPortBindings(portBindings)
                        .withVolumes(volume)
                        .withBinds(new Bind(filePath, volume))
                        .withNetworkMode("mynetwork-net")
                        .exec();
            }

            // Start container
            dockerClient.startContainerCmd(container.getId()).exec();
            System.out.println("SQL container is running successfully");
            // Wait for the container to finish (optional)
//            dockerClient.waitContainerCmd(container.getId())
//                    .exec(new WaitContainerResultCallback())
//                    .awaitCompletion();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
