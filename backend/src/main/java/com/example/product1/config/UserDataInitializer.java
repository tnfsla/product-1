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
        String adminPassword = "1234"; // Raw password for initialization

        User adminUser = userService.getUserByUsername(adminUsername);
        if (adminUser == null) {
            System.out.println("Creating default admin user...");
            User newAdmin = new User(adminUsername, adminPassword); // Password will now be saved as plain text
            userService.saveUser(newAdmin);
            System.out.println("Default admin user created: " + adminUsername);
        } else {
            System.out.println("Admin user already exists: " + adminUsername);
        }
    }
}
