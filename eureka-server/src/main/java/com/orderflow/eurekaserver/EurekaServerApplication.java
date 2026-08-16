package com.orderflow.eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

// This app IS the registry -- it does not register itself with anyone.
// Every other service (auth, order, inventory, payment, notification,
// gateway) will register here on startup and poll here to find each
// other's current host:port, instead of hardcoding addresses.
@EnableEurekaServer
@SpringBootApplication
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
