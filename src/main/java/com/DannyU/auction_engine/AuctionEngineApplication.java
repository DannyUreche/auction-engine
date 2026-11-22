package com.DannyU.auction_engine;

import com.DannyU.auction_engine.entity.User;
import com.DannyU.auction_engine.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AuctionEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuctionEngineApplication.class, args);
	}

	// Acest bloc de cod se va executa automat la fiecare pornire a aplicatiei
	@Bean
	public CommandLineRunner testDatabaseConnection(UserRepository userRepository) {
		return args -> {
			System.out.println("--- INCEPERE TEST BAZA DE DATE ---");

			// Verificam daca utilizatorul test exista deja ca sa nu dea eroare de "unique"
			if (!userRepository.existsByUsername("danny_test")) {
				User testUser = new User();
				testUser.setUsername("danny_test");
				testUser.setEmail("danny@test.com");

				// Salvam in baza de date
				userRepository.save(testUser);
				System.out.println("Utilizator salvat cu succes: " + testUser.getUsername());
			} else {
				System.out.println("Utilizatorul 'danny_test' exista deja in baza de date.");
			}

			// Afisam toti utilizatorii din baza de date
			System.out.println("Numar total de utilizatori: " + userRepository.count());

			System.out.println("--- SFARSIT TEST BAZA DE DATE ---");
		};
	}
}