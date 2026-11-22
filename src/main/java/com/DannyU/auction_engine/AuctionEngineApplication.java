package com.DannyU.auction_engine;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

@SpringBootApplication
public class AuctionEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuctionEngineApplication.class, args);
	}

	@Bean
	public CommandLineRunner testDatabaseConnection(UserRepository userRepository, ItemRepository itemRepository) {
		return args -> {
			System.out.println("--- INCEPERE TEST BAZA DE DATE ---");

			// 1. Asiguram existenta utilizatorului
			if (!userRepository.existsByUsername("danny_test")) {
				User testUser = new User();
				testUser.setUsername("danny_test");
				testUser.setEmail("danny@test.com");
				userRepository.save(testUser);
			}

			// 2. Extragem utilizatorul din baza de date
			User seller = userRepository.findByUsername("danny_test").orElseThrow();

			// 3. Verificam daca are deja un item (ca sa nu tot adaugam la fiecare restart)
			if (itemRepository.count() == 0) {
				Item auctionItem = new Item();
				auctionItem.setName("Laptop Gaming Asus ROG");
				auctionItem.setDescription("Folosit putin, arata ca nou. Ruleaza orice joc!");
				auctionItem.setStartPrice(new BigDecimal("2500.00"));
				auctionItem.setCurrentPrice(new BigDecimal("2500.00"));

				// AICI ESTE MAGIA: Legam obiectul de vanzator!
				auctionItem.setSeller(seller);

				itemRepository.save(auctionItem);
				System.out.println("Obiect salvat cu succes: " + auctionItem.getName() + " - Vandut de: " + auctionItem.getSeller().getUsername());
			} else {
				System.out.println("Exista deja " + itemRepository.count() + " obiect(e) in baza de date.");
			}

			System.out.println("--- SFARSIT TEST BAZA DE DATE ---");
		};
	}
}