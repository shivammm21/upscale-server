package com.upscale.upscale.controller;

import com.upscale.upscale.dto.ProjectCreate;
import com.upscale.upscale.dto.ProjectData;
import com.upscale.upscale.entity.Project;
import com.upscale.upscale.service.ProjectService;
import com.upscale.upscale.service.TokenService;
import com.upscale.upscale.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/project")
@CrossOrigin(origins = "${cross.origin.url}")
@Slf4j
public class ProjectController {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private UserService userService;

    @PostMapping("/create-project")
    public ResponseEntity<?> createProject(HttpServletRequest request, @RequestBody ProjectCreate projectCreate) {
        try {
            HashMap<String, Object> response = new HashMap<>();
            String email = tokenService.getEmailFromToken(request);

            if(projectCreate != null){

                if(projectService.getProject(email) != null){
                    if(projectService.updateProject(email,projectCreate)){
                        response.put("message",">>> Project updated successfully <<<");
                        log.info("Project Updated: " + email + " successfully");
                        response.put("Data",projectCreate);
                    }
                    response.put("message", "Project already exists");

                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
                else{

                    if(projectService.setProject(tokenService.getEmailFromToken(request),projectCreate)){
                        response.put("message",">>> Project created successfully <<<");
                        log.info("Project Created: " + email + " successfully");
                        response.put("Data",projectCreate);

                        return new ResponseEntity<>(response, HttpStatus.OK);
                    }
                    else{
                        log.error("Failed to create project: " + email + "");
                        response.put("message",">>> Failed to create project <<<");

                        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }

            }else{
                response.put("message",">>> Invalid project data <<<");
                log.error("Invalid project data: " + email + "");

                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/dashboard/{project-id}")
    public ResponseEntity<?> getDashboard(HttpServletRequest request, @PathVariable("project-id") String projectId) {
        try{
            String email = tokenService.getEmailFromToken(request);

            HashMap<String, Object> response = new HashMap<>();
            if(projectService.getProject(projectId) != null){
                Project projectData = projectService.getProject(projectId);
                
                // Convert teammate email IDs to names
                List<String> teammateNames = new ArrayList<>();
                for(String teammateEmail : projectData.getTeammates()) {
                    String teammateName = userService.getName(teammateEmail);
                    if(teammateName != null && !teammateName.isEmpty()) {
                        teammateNames.add(teammateName);
                    }
                }
                projectData.setTeammates(teammateNames);

                response.put("Data", projectData);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            response.put("message", "Project not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/list/{project-id}")
    public ResponseEntity<?> getProjectTasks(@PathVariable("project-id") String projectId) {
        try {
            HashMap<String, Object> response = new HashMap<>();
            Project project = projectService.getProject(projectId);

            if (project != null) {
                response.put("message", ">>> Project tasks fetched successfully <<<");
                response.put("tasks", project.getTasks());
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                response.put("message", ">>> Project not found <<<");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            log.error("Error fetching project tasks for project ID: " + projectId, e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
