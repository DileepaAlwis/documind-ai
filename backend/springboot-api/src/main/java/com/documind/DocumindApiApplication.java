package com.documind;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DocumindApiApplication {

    static {
        // Read and write all timestamps as UTC, whatever the machine's time zone is.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        SpringApplication.run(DocumindApiApplication.class, args);
    }
}
