package com.empleosvm.empleovm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.Banner;

@SpringBootApplication
public class EmpleovmApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(EmpleovmApplication.class);
        // Esto desactiva lo que hace explotar a Java 25
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }
}