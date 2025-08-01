package com.upscale.upscale.controller;

import com.upscale.upscale.dto.user.*;
import com.upscale.upscale.entity.workspace.Workspace;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.service.*;
import com.upscale.upscale.service.Workspace.WorkspaceService;
import com.upscale.upscale.service.project.EmailService;
import com.upscale.upscale.service.project.GoalService;
import com.upscale.upscale.service.project.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "${cross.origin.url}")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private GoalService goalService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private WorkspaceService workspaceService;

    @PostMapping("/login-user")
    public ResponseEntity<?> loginUser(@RequestBody LoginUser loginUser) {

        try{
            HashMap<String,Object> response = new HashMap<>();

            if(userService.login(loginUser)){
                response.put("status", "success");
                response.put("user", loginUser.getEmail());

                response.put("isNewUser", false);

                String token = tokenService.generateToken(loginUser.getEmail());
                response.put("token", token);

                return new ResponseEntity<>(response, HttpStatus.OK);

            }else if(!userService.checkUserExists(loginUser.getEmail())){
                response.put("status", "error");
                response.put("message", "Email does not exist");
                response.put("isNewUser", true);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else{
                response.put("status", "fail");
                response.put("message", "Invalid email or password");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }


        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(),HttpStatus.BAD_REQUEST);
        }

    }

//    @GetMapping("/check-user/{emailId}")
//    public ResponseEntity<?> checkUserExists(@PathVariable String emailId) {
//        try{
//            HashMap<String, Object> response = new HashMap<>();
//
//            if(userService.checkUserExists(emailId)){
//                response.put("message", "User exists, Otp sent to " + emailId + " successfully. Please check your email for OTP.");
//                response.put("email", emailId);
//                response.put("isNewUser", "false");
//                UserLogin userLogin = new UserLogin();
//                userLogin.setEmailId(emailId);
//                sendOtp(userLogin);
//                return new ResponseEntity<>(response, HttpStatus.OK);
//            }
//            else{
//
//                response.put("message", "User does not exist");
//                response.put("email", emailId);
//                response.put("isNewUser", "true");
//                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
//            }
//
//
//        }catch (Exception e){
//            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody UserLogin user) {

        try {
            String emailId = user.getEmailId();
            if (emailId == null || emailId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Email ID is required");
            }

            String otp = String.valueOf(userService.generateOtp());

            User existingUser = userService.getUser(emailId);
            Map<String, String> response = new HashMap<>();

            if (existingUser == null) {
                User newUser = new User();
                newUser.setEmailId(emailId);
                newUser.setOtp(otp);
                newUser.setNewUser(true);
                // Initialize trial system for new users
                newUser.setTrial(14);
                newUser.setActive(true);
                response.put("isNewUser", "true");
                userService.save(newUser);
                log.info("User created: " + emailId + " successfully with 14-day trial "+otp);
            } else {
                existingUser.setOtp(otp);
                existingUser.setNewUser(false);
                userService.save(existingUser);
                response.put("isNewUser", "false");
                log.info("User updated: " + emailId + "suceessfully "+otp);
            }

            //emailService.sendOtpEmail(emailId, otp);


            response.put("message", "OTP sent successfully");
            response.put("email", emailId);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>("Failed to send OTP: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody UserLoginData userLoginData) {
        try {
            if (userLoginData != null) {
                String emailId = userLoginData.getEmailId();
                String otp = userLoginData.getOtp();

                if (userService.findByEmailIdAndOtp(emailId, otp)) {
                    Map<String, String> response = new HashMap<>();
                    response.put("message", "OTP verified successfully");
                    response.put("email", emailId);

                    if (userService.isNewUser(emailId)) {
                        response.put("isNewUser", "true");
                    } else {
                        response.put("isNewUser", "false");
                    }

                    // Generate JWT token
                    String token = tokenService.generateToken(emailId);
                    response.put("token", token);

                    return new ResponseEntity<>(response, HttpStatus.OK);
                } else {
                    return new ResponseEntity<>("Invalid OTP", HttpStatus.BAD_REQUEST);
                }
            }
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>("Invalid OTP", HttpStatus.BAD_REQUEST);
    }

    @PostMapping("/create-user")
    public ResponseEntity<?> createUser(HttpServletRequest request, @RequestBody UserCreate userCreate){
        try {

            //set all data;
            String email = tokenService.getEmailFromToken(request);

            User user = userService.getUserDetails(email, userCreate);

            Workspace workspace = workspaceService.createWorkspace(user.getId());

            user.setWorkspaces(workspace.getId());

            userService.save(user);

            log.info("User Updated: " + email + " successfully");
            HashMap<String, Object> response = new HashMap<>();

            response.put("message", "User created successfully");
            response.put("workspace", workspace.getName());



            response.put("Name", userCreate.getFullName());
            response.put("email", email);
            response.put("token", tokenService.generateToken(email));
            return new ResponseEntity<>(response, HttpStatus.OK);

        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo(HttpServletRequest request){
        try {

            //set all data;
            String emailId = tokenService.getEmailFromToken(request);

            User user = userService.getUser(emailId);

            if(user != null){

                Map<String, Object> response = new HashMap<>();

                response.put("Email", user.getEmailId());
                response.put("FullName", user.getFullName());
                response.put("Role", user.getRole());
                response.put("Workspaces", user.getWorkspaces());
                response.put("AsanaUsed",user.getAsanaUsed());

                return new ResponseEntity<>(response, HttpStatus.OK);
            }


        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/home")
    public ResponseEntity<?> getHome(HttpServletRequest request){
        try{
            String emailId = tokenService.getEmailFromToken(request);

            HashMap<String, Object> response = new HashMap<>();

            User user = userService.getUser(emailId);
            
            // Update trial status before returning home data
            userService.updateTrialStatus(user);

            response.put("Email", emailId);
            response.put("Time", userService.getDate());
            response.put("FullName", userService.getName(emailId));
            response.put("Role", user.getRole());

            // Add trial information
            response.put("Trial", user.getTrial());
            response.put("Active", user.isActive());

            response.put("Goal", goalService.getGoal(emailId));

            // Projects created by the user
            response.put("My Projects", userService.getProjects(emailId));

            // Projects where user is a teammate
            response.put("Teammate Projects", projectService.getProjectsAsTeammate(emailId));

            response.put("Team Mates", userService.getTeamMates(emailId));

            return new ResponseEntity<>(response, HttpStatus.OK);

        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/profile-update")
    public ResponseEntity<?> updateUserProfile(HttpServletRequest request, @RequestBody UserProfileUpdate userProfileUpdate){

        try {
            String emailId = tokenService.getEmailFromToken(request);

            HashMap<String, Object> response = new HashMap<>();

            if(userProfileUpdate != null){

                if(userService.updateUserProfile(emailId, userProfileUpdate)){

                    response.put("message", "Profile updated successfully");
                    response.put("email", emailId);
                    log.info("Profile updated successfully");
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
                else{
                    response.put("message", "Profile update failed, Not Found");
                    response.put("email", emailId);
                    log.info("Profile update failed, Not Found");
                    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
                }

            }
            else{
                response.put("message", "Profile update failed, Not Found");
                response.put("email", emailId);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

        }catch (Exception e){
            log.error(e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/get-profile")
    public ResponseEntity<?> getUserProfile(HttpServletRequest request){
        try {
            String emailId = tokenService.getEmailFromToken(request);
            HashMap<String, Object> response = new HashMap<>();

            if(userService.getUserProfileUpdate(emailId) != null){
                UserProfileUpdate userProfileUpdate = userService.getUserProfileUpdate(emailId);
                response.put("Data", userProfileUpdate);
                response.put("email", emailId);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else{
                response.put("message", "User not found");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/decrease-trial")
    public ResponseEntity<?> decreaseTrial(HttpServletRequest request) {
        try {
            String emailId = tokenService.getEmailFromToken(request);
            HashMap<String, Object> response = new HashMap<>();

            if (userService.decreaseTrialDay(emailId)) {
                User user = userService.getUser(emailId);
                response.put("message", "Trial decreased successfully");
                response.put("trial", user.getTrial());
                response.put("active", user.isActive());
                response.put("status", user.isActive() ? "Active" : "Expired");
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                response.put("message", "Failed to decrease trial or trial already at 0");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/trial-info")
    public ResponseEntity<?> getTrialInfo(HttpServletRequest request) {
        try {
            String emailId = tokenService.getEmailFromToken(request);
            HashMap<String, Object> response = userService.getTrialInfo(emailId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
