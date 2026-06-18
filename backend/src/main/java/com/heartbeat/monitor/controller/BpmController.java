package com.heartbeat.monitor.controller;

import com.heartbeat.monitor.model.Alert;
import com.heartbeat.monitor.model.BpmRecord;
import com.heartbeat.monitor.model.User;
import com.heartbeat.monitor.repository.AlertRepository;
import com.heartbeat.monitor.repository.BpmRecordRepository;
import com.heartbeat.monitor.repository.UserRepository;
import com.heartbeat.monitor.security.UserDetailsImpl;
import com.heartbeat.monitor.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/bpm")
public class BpmController {

    @Autowired
    private BpmRecordRepository bpmRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @PostMapping
    public ResponseEntity<?> receiveBpmData(@RequestBody BpmRequest bpmRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        Long userId = userDetails.getId();

        Integer bpm = bpmRequest.getBpm();

        // 1. Save BPM Record
        BpmRecord record = new BpmRecord(userId, bpm);
        bpmRepository.save(record);

        // 2. Check Logic & Alerts
        if (bpm < 60 || bpm > 100) {
            String type = bpm < 60 ? "LOW" : "HIGH";
            String subject = type.equals("LOW") ? "Low Heart Rate Alert" : "High Heart Rate Alert";
            String msg = type.equals("LOW")
                    ? "Your heart rate is below normal (" + bpm + " BPM). Please rest and consult doctor if persists."
                    : "Your heart rate is above normal (" + bpm + " BPM). Please relax and hydrate.";

            // Save Alert to DB
            Alert alert = new Alert(userId, type, msg);
            alertRepository.save(alert);

            // Fetch User Email and Send Email Async
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                emailService.sendAlertEmail(userOpt.get().getEmail(), subject, msg);
            }
        }

        return ResponseEntity.ok(new MessageResponse("BPM data recorded successfully"));
    }

    @PostMapping("/device")
    public ResponseEntity<?> receiveBpmDataFromDevice(@RequestBody DeviceBpmRequest request) {
        System.out.println(">>> [DEVICE REQUEST] Email: " + request.getEmail() + ", BPM: " + request.getBpm());
        
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (!userOpt.isPresent()) {
            System.out.println(">>> [DEVICE REQUEST ERROR] User not found for email: " + request.getEmail());
            return ResponseEntity.badRequest().body(new MessageResponse("Error: User with this email not found."));
        }
        User user = userOpt.get();
        Long userId = user.getId();
        Integer bpm = request.getBpm();

        // 1. Save BPM Record
        BpmRecord record = new BpmRecord(userId, bpm);
        bpmRepository.save(record);

        // 2. Check Logic & Alerts
        if (bpm < 60 || bpm > 100) {
            String type = bpm < 60 ? "LOW" : "HIGH";
            String subject = type.equals("LOW") ? "Low Heart Rate Alert" : "High Heart Rate Alert";
            String msg = type.equals("LOW")
                    ? "Your heart rate is below normal (" + bpm + " BPM). Please rest and consult doctor if persists."
                    : "Your heart rate is above normal (" + bpm + " BPM). Please relax and hydrate.";

            // Save Alert to DB
            Alert alert = new Alert(userId, type, msg);
            alertRepository.save(alert);

            // Fetch User Email and Send Email Async
            emailService.sendAlertEmail(user.getEmail(), subject, msg);
        }

        return ResponseEntity.ok(new MessageResponse("BPM data recorded successfully from device"));
    }

    @GetMapping("/today")
    public ResponseEntity<?> getTodayRecords() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        Long userId = userDetails.getId();

        List<BpmRecord> records = bpmRepository.findTodayRecordsByUserId(
                userId,
                LocalDate.now().atStartOfDay());

        // If server/client dates are out of sync, still return recent saved records
        // so the dashboard can show data instead of staying blank.
        if (records.isEmpty()) {
            records = bpmRepository.findByUserIdOrderByTimestampDesc(userId);
        }

        return ResponseEntity.ok(records);
    }

    @GetMapping("/notify-check")
    public ResponseEntity<?> notifyCheckStarted() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        Long userId = userDetails.getId();

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String subject = "Heart Rate Monitoring Started";
            String msg = "Hello " + user.getFirstName()
                    + ",\n\nYour heart rate is checking now. Your dashboard is successfully receiving live data.";
            emailService.sendAlertEmail(user.getEmail(), subject, msg);
        }

        return ResponseEntity.ok(new MessageResponse("Notification Sent"));
    }
    @GetMapping("/device")
public ResponseEntity<?> getDeviceData(@RequestParam String email) {

    Optional<User> userOpt = userRepository.findByEmail(email);
    if (!userOpt.isPresent()) {
        return ResponseEntity.badRequest().body("User not found");
    }

    Long userId = userOpt.get().getId();

    List<BpmRecord> records = bpmRepository.findByUserIdOrderByTimestampDesc(userId);

    return ResponseEntity.ok(records);
}
}
