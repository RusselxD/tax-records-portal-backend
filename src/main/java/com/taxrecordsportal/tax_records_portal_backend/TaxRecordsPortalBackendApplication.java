package com.taxrecordsportal.tax_records_portal_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TaxRecordsPortalBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaxRecordsPortalBackendApplication.class, args);
		System.out.println("Compiled Successfully! :)) ");
	}

}
