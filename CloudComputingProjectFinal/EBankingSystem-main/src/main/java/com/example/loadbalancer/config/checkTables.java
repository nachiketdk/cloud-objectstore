//package com.example.loadbalancer.config;
//
//import java.io.InputStream;
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.SQLException;
//import java.sql.Statement;
//import java.util.Properties;
//
//public class checkTables {
//    private static final String CONFIG_FILE = "application.properties";
//
//    private Properties properties;
//
//    public checkTables(){
//        loadProperties();
//    }
//    public String getDatabaseUrl() {
//        return properties.getProperty("spring.datasource.url");
//    }
//
//    public String getDatabaseUsername() {
//        return properties.getProperty("spring.datasource.username");
//    }
//
//    public String getDatabasePassword() {
//        return properties.getProperty("spring.datasource.password");
//    }
//    private void loadProperties() {
//        properties = new Properties();
//
//        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
//            if (input == null) {
//                System.out.println("Sorry, unable to find " + CONFIG_FILE);
//                return;
//            }
//
//            // Load a properties file from class path, inside static method
//            properties.load(input);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void run() {
//        checkTables appConfig = new checkTables();
//
//        String jdbcUrl = appConfig.getDatabaseUrl();
//        String username = appConfig.getDatabaseUsername();
//        String password = appConfig.getDatabasePassword();
//        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
//            // Create a statement object
//            Statement statement = connection.createStatement();
//
//            // Define your table creation SQL statement
//            String createTableSQL = "CREATE TABLE admin (email VARCHAR(255) NOT NULL,\n" +
//                    "    password VARCHAR(255) NOT NULL,\n" +
//                    "    company VARCHAR(255),\n" +
//                    "    PRIMARY KEY (email, password))";
//
//            // Execute the SQL statement
//            statement.executeUpdate(createTableSQL);
//
//            System.out.println("Table created (if not exists) successfully!");
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
//}
