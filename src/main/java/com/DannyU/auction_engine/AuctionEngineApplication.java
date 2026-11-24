package com.DannyU.auction_engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync // emailurile se trimit in fundal, fara sa intarzie raspunsul
@EnableScheduling // Adauga aceasta linie pentru a permite rularea automata
public class AuctionEngineApplication {
    public static void main(String[] args) {
        // Toate orele (endTime, createdAt etc.) se lucreaza si se salveaza in UTC, oriunde ruleaza serverul.
        // Interfata le converteste in ora locala a fiecarui vizitator (vezi parseDate in frontend/src/api.js).
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(AuctionEngineApplication.class, args);
    }
}