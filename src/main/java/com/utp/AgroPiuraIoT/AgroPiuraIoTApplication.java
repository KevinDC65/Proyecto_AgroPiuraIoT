package com.utp.AgroPiuraIoT;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgroPiuraIoTApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgroPiuraIoTApplication.class, args);
	}

}
