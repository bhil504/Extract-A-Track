package com.BhillionDollarApps.extrack_a_track.services;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import com.BhillionDollarApps.extrack_a_track.models.LoginUser;
import com.BhillionDollarApps.extrack_a_track.models.User;
import com.BhillionDollarApps.extrack_a_track.repositories.UserRepository;

@Service
public class UserService {

    private static final Logger logger = Logger.getLogger(UserService.class.getName());

    @Autowired
    private UserRepository uRepo;

    // Retrieve all users from the database.
    public List<User> allUsers() {
        logger.info("Fetching all users from the database.");
        return uRepo.findAll();
    }

    // Register a new user, ensuring the email is unique and the password is hashed.
    public User register(User newUser, BindingResult result) {
        logger.info("Attempting to register user with email: " + newUser.getEmail());
        Optional<User> potentialUser = uRepo.findByEmail(newUser.getEmail());

        if (potentialUser.isPresent()) {
            result.rejectValue("email", "Matches", "An account with this email already exists.");
            logger.warning("Registration failed: email already exists - " + newUser.getEmail());
        }

        if (!newUser.getPassword().equals(newUser.getConfirm())) {
            result.rejectValue("confirm", "Matches", "The Confirm Password must match Password!");
            logger.warning("Registration failed: password and confirm password do not match.");
        }

        // Check for validation errors before saving the user
        if (result.hasErrors()) {
            logger.warning("Validation errors during registration: " + result.getAllErrors());
            return null;  // Return null if there are validation errors
        }

        // Hash the password and save the user
        String hashedPass = BCrypt.hashpw(newUser.getPassword(), BCrypt.gensalt(10));
        newUser.setPassword(hashedPass);
        logger.info("New user registered with email: " + newUser.getEmail());

        return uRepo.save(newUser);
    }

    // Retrieves a user by their ID.
    public User getUserByID(Long id) {
        Optional<User> optionalUser = uRepo.findById(id);
        if (optionalUser.isPresent()) {
            logger.info("User found with ID: " + id);
            return optionalUser.get();
        } else {
            logger.warning("User with ID " + id + " not found.");
            return null;
        }
    }

    // Updates an existing user, ensuring their password is hashed.
    public User updateUser(User u) {
        logger.info("Updating user with ID: " + u.getId());

        Optional<User> existingUser = uRepo.findById(u.getId());
        if (existingUser.isPresent() && !BCrypt.checkpw(u.getPassword(), existingUser.get().getPassword())) {
            // Hash the password if it hasn't been hashed yet
            u.setPassword(BCrypt.hashpw(u.getPassword(), BCrypt.gensalt(10)));
            logger.info("Password hashed for user ID: " + u.getId());
        }

        return uRepo.save(u);
    }

    // Deletes a user by their ID.
    public void deleteUser(Long id) {
        logger.info("Deleting user with ID: " + id);
        if (uRepo.existsById(id)) {
            uRepo.deleteById(id);
            logger.info("User with ID " + id + " deleted successfully.");
        } else {
            logger.warning("User deletion failed: no user with ID " + id);
        }
    }

    // Logs in a user by checking their email and password.
    public User login(LoginUser loginUser, BindingResult result) {
        logger.info("Attempting to log in user with email: " + loginUser.getEmail());

        Optional<User> potentialUser = uRepo.findByEmail(loginUser.getEmail());

        if (!potentialUser.isPresent()) {
            result.rejectValue("email", "NotFound", "Email not found.");
            logger.warning("Login failed: email not found - " + loginUser.getEmail());
            return null;
        }

        User user = potentialUser.get();

        // Check if the password matches the hashed password
        if (!BCrypt.checkpw(loginUser.getPassword(), user.getPassword())) {
            result.rejectValue("password", "Invalid", "Invalid password.");
            logger.warning("Login failed: invalid password for email - " + loginUser.getEmail());
            return null;
        }

        logger.info("User logged in successfully with email: " + loginUser.getEmail());
        return user;
    }

    // Method to retrieve a user by ID
    public Optional<User> findUserByID(Long id) {
        logger.info("Retrieving user by ID: " + id);
        return uRepo.findById(id);
    }
}
