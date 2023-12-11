package com.ebanking.service1;

import com.ebanking.service1.config.checkTables;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

//@EnableJpaRepositories("com.ebanking.service1")
//@ComponentScan(basePackages = {"com.example.ebanking"})
@EntityScan("com.ebanking.service1.entities")
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class })
public class DemoApplication implements WebMvcConfigurer {

	public static void main(String[] args) {
//		checkTables ct = new checkTables();
//		ct.run()
		SpringApplication.run(DemoApplication.class, args);
//		LoadBalancer loadBalancer = new LoadBalancer();
//		loadBalancer.run();
//		DockerService ds = new DockerService();
//		ds.run();
		System.out.println("Run Successful");
	}
}
