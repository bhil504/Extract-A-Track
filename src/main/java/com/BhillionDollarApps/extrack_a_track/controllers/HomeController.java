package com.BhillionDollarApps.extrack_a_track.controllers;

import com.BhillionDollarApps.extrack_a_track.models.LoginUser;
import com.BhillionDollarApps.extrack_a_track.models.Tracks;
import com.BhillionDollarApps.extrack_a_track.models.User;
import com.BhillionDollarApps.extrack_a_track.services.UserService;
import com.BhillionDollarApps.extrack_a_track.services.TracksService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.logging.Logger;

@Controller
public class HomeController {

    private static final Logger logger = Logger.getLogger(HomeController.class.getName());

    @Autowired
    private UserService userService;
    @Autowired
    private TracksService tracksService;
    @Autowired
    private HttpSession session;

    // Login and Registration Page
    @GetMapping("/")
    public String index(Model model) {
        if (session.getAttribute("userId") != null) {
            return "redirect:/welcome";
        }
        model.addAttribute("newUser", new User());
        model.addAttribute("newLogin", new LoginUser());
        return "LoginAndRegister";
    }
    
    @GetMapping("/login")
    public String LoginRegister(Model model) {
        if (session.getAttribute("userId") != null) {
            return "redirect:/welcome";
        }
        model.addAttribute("newUser", new User());
        model.addAttribute("newLogin", new LoginUser());
        return "LoginAndRegister";
    }


    // Registration Handling
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("newUser") User user, BindingResult result, 
                               Model model, HttpSession session) {
        // Attempt to register the user, handling unique email check within the register method
        User registeredUser = userService.register(user, result);

        // Check for validation errors
        if (result.hasErrors()) {
            model.addAttribute("newLogin", new LoginUser()); // Re-populate login form on error
            return "LoginAndRegister"; // Returns to the registration form if there are errors
        }

        // If registration is successful, add the user ID to the session
        session.setAttribute("userId", registeredUser.getId()); // Assuming `getId()` returns the user ID

        // Redirect to the welcome page
        return "redirect:/welcome";
    }


    // Login Handling
    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("newLogin") LoginUser newLogin,
                        BindingResult result, Model model) {
        logger.info("Attempting to log in user with email: " + newLogin.getEmail());

        User user = userService.login(newLogin, result);
        
        // Check for validation errors or if login fails
        if (result.hasErrors() || user == null) {
            model.addAttribute("newUser", new User()); // Re-populate registration form on error
            return "LoginAndRegister";
        }

        session.setAttribute("userId", user.getId());
        session.setMaxInactiveInterval(1800); // Set session timeout (in seconds)
        return "redirect:/welcome"; 
    }

    // Logout Handling
    @GetMapping("/logout")
    public String logout() {
        logger.info("Logging out user with ID: " + session.getAttribute("userId"));
        session.invalidate(); // Invalidate the session
        return "redirect:/"; // Redirect to login page
    }

    // The Logged-in User's Dashboard
    @GetMapping("/welcome")
    public String dashboard(Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/"; // Redirect to login if no user is logged in
        }

        User user = userService.getUserByID(userId);
        if (user == null) {
            session.invalidate();
            return "redirect:/"; // If user no longer exists, redirect to login
        }

        model.addAttribute("user", user);
        List<Tracks> userTracks = tracksService.findTracksByUserId(userId);
        model.addAttribute("userTracks", userTracks);

        return "Dashboard";
    }
}
