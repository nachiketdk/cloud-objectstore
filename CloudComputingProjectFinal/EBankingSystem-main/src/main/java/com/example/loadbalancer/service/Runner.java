package com.example.loadbalancer.service;
import com.github.dockerjava.api.model.Container;

import java.io.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Runner {
    DockerAgent dockerAgent;
    String currentUserEmail;
    String currentUserCompanyName="unknown";

    Boolean[] containers;
    int[] countOfInstances;

    static String url = "http://localhost:8080/";


    Runner() {
        this.currentUserEmail = null;
        this.currentUserCompanyName = null;
        this.dockerAgent = new DockerAgent();
        this.containers = new Boolean[5];
        this.countOfInstances = new int[5];
        Arrays.fill(containers, Boolean.FALSE);
    }

    public static void main(String[] args) throws IOException {
        Runner runner = new Runner();
        Scanner sc = new Scanner(System.in);
        URL apiUrl = new URL(url);


        while (true) {
            runner.printMenu();
            System.out.print("Enter your choice: ");
            int choice = sc.nextInt();
            sc.nextLine();
            switch (choice) {
                case 1:
                    boolean successSignup = runner.signup(sc);
                    if(successSignup) runner.dashboard(runner,sc);
                    break;
                case 2:
                    boolean successLogin = runner.login(sc);
                    if(successLogin) runner.dashboard(runner,sc);
                    break;
                case 3:
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid Choice");
            }
        }
    }

    private void dashboard(Runner runner,Scanner sc) {
        boolean noExit = true;
        while (noExit) {
            runner.printDashboard();
            System.out.print("Enter your choice: ");
            int choice = sc.nextInt();
            sc.nextLine();
            switch (choice) {
                case 1:
                    runner.buildAllImages(5);
                    break;
                case 2:
                    runner.startService(runner.serviceDashboard(sc,false));
                    break;
                case 3:
                    runner.stopService(runner.serviceDashboard(sc,true));
                    break;
                case 4:

                    runner.changeLoadBalancerConfiguration(sc);
                    break;
                case 5:
                    runner.changeAutoScalerConfiguration(sc);
                    break;
                case 6:
                    runner.listAllRunningServices();
                    break;
                case 7:
                    runner.startSql();
                    break;
                case 8:
                    noExit = false;
                    break;
                default:
                    System.out.println("Invalid Choice");
            }
        }
    }

    private List<Container> listAllRunningServices() {
        List<Container> allContainers = dockerAgent.listAllContainers(false);
        Arrays.fill(countOfInstances,0);

        for(Container eachContainer: allContainers) {
            String containerId = eachContainer.getNames()[0];
            String[] parts = containerId.split("-");
            if(parts[0].equals("/docker"))continue;

//            System.out.println("DEBUG: " + parts[0] );
//            System.out.println("DEBUG: " + parts[2]);
            if (parts[0].equals("/"+currentUserCompanyName)) {
//                System.out.println("Running Service: " + parts[2]);
                this.containers[Integer.parseInt(parts[2]) - 1] = true;
                this.countOfInstances[Integer.parseInt(parts[2]) - 1]+=1;
            }
        }
        for(int i=0;i<5;i++){
            if(this.containers[i]){
                System.out.println("Running Service: "+(i+1)+" Current Instances: "+this.countOfInstances[i]);
            }
        }
        return allContainers;
    }

    private boolean changeAutoScalerConfiguration(Scanner sc) {

        System.out.println("########### SelectService for AutoScaler ############");
        for(int i=1;i<=this.containers.length;i++){
            if(this.containers[i-1]){
                System.out.printf("%d. Service%d \n",i,i);
            }
        }

        System.out.print("Enter Choice: ");
        int serviceNo = sc.nextInt();
        sc.nextLine();

        if(serviceNo<=0 || !this.containers[serviceNo-1]){
            System.out.println("Invalid Selection");
            return true;
        }

        String as_strategy;
        int as = autoScalerDashboard(sc);
        sc.nextLine();

        if(as==0)return true;

        switch (as){
            case(1) :
                as_strategy = "threshold";
                break;
            case(2) :
                as_strategy = "timeseries";
                break;
            default:
                as_strategy = "null";
                break;
        }
        try{
            String urlWithParams = url + "settings/autoscaler?strategy=" + as_strategy + "&username=" + currentUserCompanyName +"&service=" + serviceNo;
            URL apiUrl = new URL(urlWithParams);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("POST");
            int responseCode = connection.getResponseCode();

//            connection.setRequestMethod("POST");
//                connection.setDoOutput(true);
//                connection.getOutputStream().write(data.getBytes());
//                responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                System.out.println("Auto Scaler Strategy Applied ");
                return true;
            } else {
                System.out.println("Auto Scaler Strategy Failed to apply");
                return true;
            }

        } catch (Exception e) {
            System.out.println("Auto Scaler Strategy Failed to apply");
            System.out.println(e.getMessage());
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
        return false;

    }

    private boolean changeLoadBalancerConfiguration(Scanner sc) {

        System.out.println("########### Select Service for Load Balancer ############");
        for(int i=1;i<=this.containers.length;i++){
            if(this.containers[i-1]){
                System.out.printf("%d. Service%d \n",i,i);
            }
        }

        System.out.print("Enter Choice: ");
        int serviceNo = sc.nextInt();
        sc.nextLine();

        if(serviceNo<=0 || !this.containers[serviceNo-1]){
            System.out.println("Invalid Selection");
            return true;
        }

        String lb_strategy;
        int[] weights = null;
        int lb = loadBalancerDashBoard(sc);
        sc.nextLine();

        if(lb==0)return true;
        switch (lb){
            case(3) :
                lb_strategy = "weightedRoundRobin";
                 weights = this.weightSelectionDashboard(sc,serviceNo);
                break;
            case(1) :
                lb_strategy = "random";
                break;
            case(4) :
                lb_strategy = "weightedLeastConnection";
                weights = this.weightSelectionDashboard(sc,serviceNo);

                break;
            case(5) :
                lb_strategy = "ipHash";
                break;
            case (2):
                lb_strategy = "powerOfTwoChoices";
                break;
            default:
                lb_strategy = "weightedRoundRobin";
                break;
        }
        String urlWithParams;
        StringBuilder weightsString = new StringBuilder();
        try{
            if(weights==null) {
                urlWithParams = url + "settings/loadbalancer?strategy=" + lb_strategy + "&username=" + currentUserCompanyName +"&service=" + serviceNo ;
            }
            else {
                for(int i=0;i<weights.length;i++){
                    weightsString.append(weights[i]);
                    if(i!=weights.length-1)
                        weightsString.append(",");
                }
                urlWithParams = url + "settings/loadbalancer?strategy=" + lb_strategy + "&username=" + currentUserCompanyName +"&service=" + serviceNo+"&weights="+weightsString.toString() ;
            }
            URL apiUrl = new URL(urlWithParams);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("POST");
            int responseCode = connection.getResponseCode();

//            connection.setRequestMethod("POST");
//                connection.setDoOutput(true);
//                connection.getOutputStream().write(data.getBytes());
//                responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                System.out.println("Load Balancer Strategy Applied");
                return true;
            } else {
                System.out.println("Load Balancer Strategy Failed to apply");
                return true;
            }

        } catch (Exception e) {
            System.out.println("Load Balancer Strategy Failed to apply");
            System.out.println(e.getMessage());
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
        return false;

    }

    private int[] weightSelectionDashboard(Scanner sc,int serviceNo) {
        System.out.println("Default Weights is set to 1 for each container");
        System.out.println("Enter 1 to continue . Enter 0 to  change weights. ");
        int n = sc.nextInt();
        sc.nextLine();

        if(n!=0)return null;
        List<Container> allContainers = listAllRunningServices();
        int instanceCount=0;
        for(Container eachContainer: allContainers){
            String containerId = eachContainer.getNames()[0];
            String[] parts = containerId.split("-");
            if (parts[0].equals("/"+currentUserCompanyName)&&Integer.parseInt(parts[2])==serviceNo) {
                instanceCount+=1;
            }
        }
        System.out.println("Current instance of your container: "+instanceCount);
        System.out.println("Enter: "+instanceCount+" many integers for the weights");
        int[] weights = new int[instanceCount];
        for(int i=0;i<instanceCount;i++){
            weights[i] = sc.nextInt();
        }
        sc.nextLine();
        return weights;
    }

    private boolean stopService(String[] containers) {
        StringBuilder containerString = new StringBuilder();
        int count=0;
        for(String container: containers){
            containerString.append(container);
            if(count<containers.length-1)
                containerString.append(",");
            count++;
        }
        if(containerString.toString().equals("0"))return true;

        try{
            String urlWithParams = url + "settings/stop?services=" + containerString + "&username=" + currentUserCompanyName;
            URL apiUrl = new URL(urlWithParams);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("POST");
            int responseCode = connection.getResponseCode();

//            connection.setRequestMethod("POST");
//                connection.setDoOutput(true);
//                connection.getOutputStream().write(data.getBytes());
//                responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                for(String i: containers){
                    this.containers[Integer.parseInt(i)-1] = true;
                }
                System.out.println("Services Stopped");
                return true;
            } else {
                System.out.println("Services could not stop");
                return true;
            }

        } catch (Exception e) {
            System.out.println("Services could not stop");
            System.out.println(e.getMessage());
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
        return false;
    }

    private boolean startService(String[] containers) {
        StringBuilder containerString = new StringBuilder();
        int count=0;
        for(String container: containers){
            containerString.append(container);
            if(count<containers.length-1)
                containerString.append(",");
            count++;
        }
        if(containerString.toString().equals("0"))return true;

        try{
            String urlWithParams = url + "settings/start?services=" + containerString + "&username=" + currentUserCompanyName;
            URL apiUrl = new URL(urlWithParams);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("POST");
            int responseCode = connection.getResponseCode();

//            connection.setRequestMethod("POST");
//                connection.setDoOutput(true);
//                connection.getOutputStream().write(data.getBytes());
//                responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                for(String i: containers){
                    this.containers[Integer.parseInt(i)-1] = true;
                }
                System.out.println("Services Started");
                System.out.println("Default LoadBalancer Strategy: WeightedRoundRobin");
                System.out.println("Autoscaler is disabled initially: Start from Main Menu");
                return true;
            } else {
                System.out.println("Services could not start");
                return false;
            }

        } catch (Exception e) {
            System.out.println("Services could not start");
            System.out.println(e.getMessage());
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
        return false;
    }

    private void buildAllImages(int n) {
        dockerAgent.buildAllImages(n);
    }

    public void printMenu() {
        clearConsole();
        System.out.println("########### Load Balancer and Autoscaler ############");
        System.out.println("1. Signup");
        System.out.println("2. Login");
        System.out.println("3. Exit");
    }
    public String[] serviceDashboard(Scanner sc,boolean stopService) {
        if(stopService)
        {
            this.listAllRunningServices();
            System.out.println("########### Select Service To Stop ############");
            for(int i=1;i<=this.containers.length;i++){
                if(this.containers[i-1]){
                    System.out.printf("%d. Service%d \n",i,i);
                }
            }
            System.out.println("0. Back");
            System.out.println("\n Example Enter \"1 2 3\" to stop all three services. Make sure that service is already running");
        }
        else{
            System.out.println("########### Select Service To Start ############");
            System.out.println("1. Service1");
            System.out.println("2. Service2");
            System.out.println("3. Service3");
            System.out.println("4. Service4");
            System.out.println("5. Service5");
            System.out.println("0. Back");
            System.out.println("\n Example Enter \"1 2 3\" to start all three services");

        }
        System.out.print("Enter Choices: ");
        String n = sc.nextLine();
        return n.split(" ");
    }

    public int loadBalancerDashBoard(Scanner sc) {
        this.listAllRunningServices();
//        clearConsole();
        System.out.println("########### Load Balancer ############");
        System.out.println("1. Random Load Balancer");
        System.out.println("2. Power of 2 choices Load Balancer");
        System.out.println("3. Weighted Round Robin Load Balancer");
        System.out.println("4. Weighted Least Connected");
        System.out.println("5. IP Hash");
        System.out.println("0. Main Menu");

        System.out.print("Enter Choice: ");;
        return sc.nextInt();

    }

    public int autoScalerDashboard(Scanner sc) {
        this.listAllRunningServices();
//        int lb = loadBalancerDashBoard(sc);

        System.out.println("########### Load Balancer ############");
        System.out.println("1. Threshold Based Auto Scaler");
        System.out.println("2. Time Series Based Auto Scaler");
        System.out.println("3. No AutoScaler");
        System.out.println("0. Main Menu");
        System.out.print("Enter Choice: ");;
        return sc.nextInt();
    }
    public void printDashboard() {
        clearConsole();
        System.out.flush();
        System.out.println("########### Load Balancer and Autoscaler Project############");
        System.out.println("1. Build all images");
        System.out.println("2. Start a service");
        System.out.println("3. Stop a service");
        System.out.println("4. Change Load Balancer configuration of a running service");
        System.out.println("5. Change AutoScaler configuration of a running service");
        System.out.println("6. List of all running services");
        System.out.println("7. Start SQL Server");
        System.out.println("8. Logout");
    }

    private boolean login(Scanner scanner) {
        try (BufferedReader reader = new BufferedReader(new FileReader("user_data.txt"))) {
//            Scanner scanner = new Scanner(System.in);

            System.out.print("Enter your email: ");
            String email = scanner.nextLine();
            System.out.print("Enter your password: ");
            String password = scanner.nextLine();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3 && parts[0].equals(email) && parts[1].equals(password)) {
                    this.currentUserEmail = parts[0];
                    this.currentUserCompanyName = parts[2];
                    System.out.println("Login successful. Welcome, " + parts[2] + "!");
                    return true;
                }
            }


            System.out.println("Invalid email or password. Please try again.");

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }
    private boolean signup(Scanner scanner) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("user_data.txt", true))) {
//            Scanner scanner = new Scanner(System.in);

            System.out.print("Enter your email: ");
            String email = scanner.nextLine();

            System.out.print("Enter your password: ");
            String password = scanner.nextLine();

            System.out.print("Enter your company name: ");
            String company = scanner.nextLine();

            // Append user information to the file
            writer.write(email + "," + password + "," + company);
            writer.newLine();
            this.currentUserEmail = email;
            this.currentUserCompanyName = company;

            System.out.println("User signed up successfully.");
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void clearConsole() {
        try {
            final String os = System.getProperty("os.name");

            if (os.contains("Windows")) {
                // For Windows
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                // For UNIX-like systems
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (final Exception e) {
            // Handle exceptions
            e.printStackTrace();
        }
    }
    private void startSql(){
        System.out.println("Establishing MySQL Connection");
        String sqlUrl = url+"settings/startsql?username="+currentUserCompanyName;
        try{
            URL apiUrl = new URL(sqlUrl);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("POST");
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                System.out.println("MySQL Server Started");
            } else {
                System.out.println(responseCode);
                System.out.println("MySQL Server Failed to start");
            }
        }
        catch (Exception e){
            System.out.println("MySQL Server Failed to start");
            System.out.println(e.getMessage());
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
    }
}