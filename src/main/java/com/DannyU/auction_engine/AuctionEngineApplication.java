package com.DannyU.auction_engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // NOU: Activeaza sarcinile automate de fundal
public class AuctionEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuctionEngineApplication.class, args);
    }
}