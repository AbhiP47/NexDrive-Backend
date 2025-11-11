package com.appshala.emailnotificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class EmailnotificationserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EmailnotificationserviceApplication.class, args);
	}

}
