package com.heartbeat.monitor.controller;

import com.heartbeat.monitor.model.User;
import com.heartbeat.monitor.repository.UserRepository;
import com.heartbeat.monitor.security.JwtUtils;
import com.heartbeat.monitor.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.heartbeat.monitor.model.OtpVerification;
import com.heartbeat.monitor.repository.OtpVerificationRepository;
import com.heartbeat.monitor.service.EmailService;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    OtpVerificationRepository otpVerificationRepository;

    @Autowired
    EmailService emailService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getFirstName(),
                userDetails.getAge(),
                userDetails.getEmail()));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is required."));
        }
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        // Generate 6 digit OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        
        // Remove existing old OTPs for this email if any
        otpVerificationRepository.deleteByEmail(email);
        
        otpVerificationRepository.save(new OtpVerification(email, otp));
        System.out.println("\n=================================");
        System.out.println("Generated OTP for " + email + " is: " + otp);
        System.out.println("=================================\n");
        
        emailService.sendAlertEmail(email, "Your Verification Code", "Your verification code is: " + otp);

        return ResponseEntity.ok(new MessageResponse("OTP sent successfully!"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        if (signUpRequest.getAge() <= 10) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Age must be greater than 10."));
        }

        if (signUpRequest.getOtp() == null || signUpRequest.getOtp().isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: OTP is required."));
        }

        Optional<OtpVerification> otpOpt = otpVerificationRepository.findByEmailAndOtp(signUpRequest.getEmail(), signUpRequest.getOtp());
        if (otpOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid or expired OTP."));
        }

        if (!signUpRequest.getPassword().equals(signUpRequest.getConfirmPassword())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Passwords do not match."));
        }

        // Create new user's account
        User user = new User(signUpRequest.getFirstName(),
                signUpRequest.getAge(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));

        userRepository.save(user);
        
        // Delete OTP after successful registration
        otpVerificationRepository.deleteByEmail(signUpRequest.getEmail());

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }
}
