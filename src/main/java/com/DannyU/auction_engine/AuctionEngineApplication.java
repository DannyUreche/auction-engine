package com.DannyU.auction_engine;

import com.DannyU.auction_engine.entity.Item;
import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.ItemRepository;
import com.DannyU.auction_engine.repository.UserRepository;
import com.DannyU.auction_engine.service.BidService;
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
	public CommandLineRunner testDatabaseConnection(
			UserRepository userRepository,
			ItemRepository itemRepository,
			BidService bidService) {

		return args -> {
			System.out.println("--- INCEPERE SIMULARE LICITATIE ---");

			// 1. Ne asiguram ca avem vanzatorul (danny_test)
			if (!userRepository.existsByUsername("danny_test")) {
				User seller = new User();
				seller.setUsername("danny_test");
				seller.setEmail("danny@test.com");
				userRepository.save(seller);
			}
			User seller = userRepository.findByUsername("danny_test").orElseThrow();

			// 2. Ne asiguram ca avem obiectul de vanzare (Laptop)
			if (itemRepository.count() == 0) {
				Item auctionItem = new Item();
				auctionItem.setName("Laptop Gaming Asus ROG");
				auctionItem.setDescription("Ruleaza orice joc!");
				auctionItem.setStartPrice(new BigDecimal("2500.00"));
				auctionItem.setCurrentPrice(new BigDecimal("2500.00"));
				auctionItem.setSeller(seller);
				itemRepository.save(auctionItem);
			}
			Item laptop = itemRepository.findAll().get(0); // Luam primul obiect din DB

			// 3. Cream un cumparator (alex_bidder)
			if (!userRepository.existsByUsername("alex_bidder")) {
				User buyer = new User();
				buyer.setUsername("alex_bidder");
				buyer.setEmail("alex@test.com");
				userRepository.save(buyer);
			}
			User buyer = userRepository.findByUsername("alex_bidder").orElseThrow();

			// 4. ACȚIUNE: Alex incearca sa liciteze 2600.50 pentru Laptop
			try {
				System.out.println("Alex incearca sa liciteze 2600.50 pentru laptop...");
				bidService.placeBid(laptop.getId(), buyer.getId(), new BigDecimal("2600.50"));
				System.out.println("Licitatie acceptata cu succes!");

				// Verificam daca pretul laptopului a crescut in baza de date
				Item updatedLaptop = itemRepository.findById(laptop.getId()).orElseThrow();
				System.out.println("Noul pret curent al laptopului este: " + updatedLaptop.getCurrentPrice());

			} catch (Exception e) {
				System.out.println("Eroare la licitatie: " + e.getMessage());
			}

			System.out.println("--- SFARSIT SIMULARE LICITATIE ---");
		};
	}
}