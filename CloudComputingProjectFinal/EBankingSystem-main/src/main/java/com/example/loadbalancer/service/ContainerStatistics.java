package com.example.loadbalancer.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.StatsCmd;
//import com.github.dockerjava.api.model.ContainerStats;
import com.github.dockerjava.core.DockerClientBuilder;

        import java.io.IOException;
        import java.io.InputStream;
        import java.util.Scanner;

public class ContainerStatistics {

    public static void main(String[] args) {
        String containerId = "your-container-id"; // Replace with the actual container ID

        try (DockerClient dockerClient = DockerClientBuilder.getInstance().build()) {

            // Step 1: Get container statistics
            InputStream statsStream = getContainerIOStats(dockerClient, containerId);

            // Step 2: Parse the statistics and find the load
            if (statsStream != null) {
                String statsJson = readStream(statsStream);
                double containerLoad = parseContainerLoad(statsJson);

                System.out.println("Container Load: " + containerLoad);
            } else {
                System.out.println("Unable to retrieve container stats.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static InputStream getContainerIOStats(DockerClient dockerClient, String containerId) {
//        try {
//            // Use the statsCmd to get real-time I/O stats
//            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
//            return statsCmd.exec();
//        } catch (Exception e) {
//            e.printStackTrace();
            return null;
//        }
    }

    private static String readStream(InputStream inputStream) throws IOException {
        try (Scanner scanner = new Scanner(inputStream).useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    private static double parseContainerLoad(String statsJson) {
        // Parse the container statistics JSON and extract the load information
        // Adjust this based on the actual structure of the JSON data
        // For demonstration purposes, assuming it's a simple key-value pair
        // You may need to use a JSON library for more complex structures
        return Double.parseDouble(statsJson); // Adjust this based on your JSON structure
    }
}
