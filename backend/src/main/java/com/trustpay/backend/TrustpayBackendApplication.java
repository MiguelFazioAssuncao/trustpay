package com.trustpay.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TrustpayBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrustpayBackendApplication.class, args);
	}

}
