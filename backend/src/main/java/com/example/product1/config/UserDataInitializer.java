package com.example.product1.config;

import com.example.product1.model.User;
import com.example.product1.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class UserDataInitializer implements CommandLineRunner {

    private final UserService userService;

    public UserDataInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) throws Exception {
        String adminUsername = "admin";
        String adminPassword = "1234";

        User adminUser = userService.getUserByUsername(adminUsername);
        if (adminUser == null) {
            System.out.println("Admin user not found. Creating with hashed password...");
            userService.saveUser(new User(adminUsername, adminPassword));
            System.out.println("Admin user created.");
        } else if (!adminUser.getPassword().startsWith("$2")) {
            // 평문 비밀번호 감지 → 해싱 후 DB 업데이트
            System.out.println("Admin password is plain text. Re-hashing and updating DB...");
            userService.saveUser(new User(adminUsername, adminPassword));
            System.out.println("Admin password updated to hashed.");
        } else {
            System.out.println("Admin user already exists with hashed password.");
        }
    }
}
