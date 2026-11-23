package com.DannyU.auction_engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Adauga aceasta linie pentru a permite rularea automata
public class AuctionEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuctionEngineApplication.class, args);
    }
}