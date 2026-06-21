package com.kartik.terminal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KartikTerminalApplication {

	public static void main(String[] args) {
		SpringApplication.run(KartikTerminalApplication.class, args);
		System.out.println("====================================");
		System.out.println("  KartikTerminal Backend Started!  ");
		System.out.println("  http://localhost:8080            ");
		System.out.println("====================================");
	}

	@org.springframework.context.annotation.Bean
	public org.springframework.boot.CommandLineRunner initAdmin(
			com.kartik.terminal.repository.UserRepository userRepository,
			org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
		return args -> {
			String adminEmail = "kartiksharma768976@gmail.com";
			java.util.Optional<com.kartik.terminal.entity.User> existingAdmin = userRepository.findByEmail(adminEmail);

			if (existingAdmin.isPresent()) {
				com.kartik.terminal.entity.User admin = existingAdmin.get();
				if (admin.getRole() != com.kartik.terminal.entity.User.Role.ADMIN) {
					admin.setRole(com.kartik.terminal.entity.User.Role.ADMIN);
					admin.setIsActive(true);
					userRepository.save(admin);
					System.out.println("Existing user " + adminEmail + " has been promoted to ADMIN.");
				}
			} else {
				com.kartik.terminal.entity.User admin = new com.kartik.terminal.entity.User();
				admin.setUsername("kartik_admin");
				admin.setEmail(adminEmail);
				admin.setPassword(passwordEncoder.encode("kartik@2005"));
				admin.setRole(com.kartik.terminal.entity.User.Role.ADMIN);
				admin.setIsActive(true);
				admin.setFullName("Kartik Admin");
				userRepository.save(admin);
				System.out.println("New admin user created: " + adminEmail + " / kartik@2005");
			}
		};
	}
}
