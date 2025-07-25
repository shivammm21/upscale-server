package com.upscale.upscale.service.project;

import com.upscale.upscale.dto.project.*;
import com.upscale.upscale.dto.task.TaskData;
import com.upscale.upscale.entity.project.Project;
import com.upscale.upscale.entity.project.Section;
import com.upscale.upscale.entity.project.Task;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.repository.ProjectRepo;
import com.upscale.upscale.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class ProjectService {

    @Autowired
    private ProjectRepo projectRepo;

    @Autowired
    @Lazy
    private UserService userService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private InboxService inboxService;

    @Autowired
    private SectionService sectionService;

    public void save(Project project){
        projectRepo.save(project);
    }
    public Project getProject(String projectId){
        return projectRepo.findById(projectId).orElse(null);
    }

    public Task setTask(TaskData taskData, String createdId, String email) {

        log.info("Received TaskData: {}", taskData);

        Task task = new Task();
        task.setTaskName(taskData.getTaskName());
        task.setStartDate(taskData.getStartDate() != null ? taskData.getStartDate() : taskData.getDate());
        task.setEndDate(taskData.getEndDate());
        task.setDate(taskData.getDate());
        task.setCompleted(false);
        task.setCreatedId(createdId);
        task.setProjectIds(taskData.getProjectIds());
        task.setDescription(taskData.getDescription());


        List<String> assignId = new ArrayList<>();
        for(String id:taskData.getAssignId()){

            if(id != createdId){
                inboxService.sendTaskDetails(task,email,id);
            }

            User user = userService.getUser(id);

            assignId.add(user.getId());
        }

        task.setAssignId(assignId);

        Task savedTask = taskService.save(task);
        log.info("Saved Task to DB: {}", savedTask);

        return savedTask;

    }

    public HashMap<String, List<String>> getTasks(String projectId, List<String> teammates, String creatorId, HashMap<String,List<String>> tasks){
        HashMap<String, List<String>> newTasks = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : tasks.entrySet()) {
            String groupName = entry.getKey();
            List<String> taskNames = entry.getValue();

            List<String> taskIds = new ArrayList<>();
            for (String taskName : taskNames) {
                Task task = new Task();
                task.setTaskName(taskName);
                task.setGroup(groupName);
                task.setCompleted(false);
                task.setDate(new Date());
                task.setProjectIds(Collections.singletonList(projectId)); // Set project ID
                task.setAssignId(teammates); // Assign to all teammates
                task.setCreatedId(creatorId); // Set the creator ID
                // Save the task to get an ID
                Task savedTask = taskService.save(task);
                taskIds.add(savedTask.getId());
            }

            newTasks.put(groupName, taskIds);
        }

        return newTasks;
    }

    public List<Section> getSections(String projectId, List<String> teammates, String creatorId, HashMap<String, List<String>> tasksMap) {
        List<Section> sections = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : tasksMap.entrySet()) {
            String sectionName = entry.getKey();
            List<String> taskNames = entry.getValue();

            Section section = new Section();
            section.setId(UUID.randomUUID().toString());
            section.setSectionName(sectionName);
            List<Task> taskList = new ArrayList<>();

            for (String taskName : taskNames) {
                Task task = new Task();
                task.setTaskName(taskName);
                task.setGroup(sectionName);
                task.setCompleted(false);
                task.setDate(new Date());
                task.setProjectIds(Collections.singletonList(projectId));
                task.setAssignId(teammates);
                task.setCreatedId(creatorId);

                Task savedTask = taskService.save(task);
                taskList.add(savedTask);
            }

            section.setTasks(taskList);
            //sectionService.save(section);
            sections.add(section);
        }

        return sections;
    }

    public boolean setProject(String emailId, ProjectCreate projectCreate) {
        if(emailId.isEmpty()) return false;

        Project newProject = new Project();

        newProject.setUserEmailid(emailId);
        newProject.setProjectName(projectCreate.getProjectName());
        newProject.setWorkspace(projectCreate.getWorkspace());

        // Save the project first to get an ID before setting tasks
        save(newProject);

        // Get the creator's ID
        String creatorId = userService.getUser(emailId).getId();

        // Now pass the project ID, teammates, and creator ID to getTasks
        List<Section> sections = getSections(newProject.getId(), projectCreate.getTeammates(), creatorId, projectCreate.getTasks());
        newProject.setSection(sections);

        newProject.setLayouts(projectCreate.getLayouts());
        newProject.setRecommended(projectCreate.getRecommended());
        newProject.setPopular(projectCreate.getPopular());
        newProject.setOther(projectCreate.getOther());

        // Process teammates and send invitations
        List<String> validTeammates = new ArrayList<>();
        for (String teammate : projectCreate.getTeammates()) {
            // Check if teammate exists in database
            User teammateUser = userService.getUser(teammate);
            if (teammateUser != null) {
                validTeammates.add(teammate);
                // Send project invitation via inbox
                inboxService.sendProjectInvite(emailId, teammate, newProject);
            } else {
                log.warn("Teammate not found in database: {}", teammate);
            }
        }
        newProject.setTeammates(validTeammates);



        save(newProject); // Save again with updated tasks and teammates
        return userService.setProject(newProject, emailId);
    }


    public boolean updateProject(String emailId, ProjectCreate projectCreate){
        Project project = getProject(emailId);

        if(project != null){

            if(project.getLayouts().isEmpty()) project.setLayouts(projectCreate.getLayouts());
            if(project.getRecommended().isEmpty()) project.setRecommended(projectCreate.getRecommended());
            if(project.getPopular().isEmpty()) project.setPopular(projectCreate.getPopular());
            if(project.getOther().isEmpty()) project.setOther(projectCreate.getOther());
            if(project.getTeammates().isEmpty()) project.setTeammates(projectCreate.getTeammates());

            save(project);
            return true;
        }

        return false;
    }

    public ProjectData getInfo(String emailId){
        Project project = getProject(emailId);
        ProjectData projectData = new ProjectData();
        projectData.setProjectName(project.getProjectName());
        projectData.setWorkspace(project.getWorkspace());
        
        // This method now needs to resolve task IDs to Task objects.
        // For now, I'll leave it empty as the main focus is the /list/{project-id} endpoint.
        // projectData.setTasks(...); 
        
        projectData.setLayouts(project.getLayouts());
        projectData.setRecommended(project.getRecommended());
        projectData.setPopular(project.getPopular());
        projectData.setOther(project.getOther());
        projectData.setTeammates(project.getTeammates());
        return projectData;
    }

    public String getProjectName(String projectId){
        return projectRepo.findById(projectId).get().getProjectName();
    }

    public HashMap<String, String> getProjectsAsTeammate(String emailId) {
        HashMap<String, String> teammateProjects = new HashMap<>();
        List<Project> allProjects = projectRepo.findAll();
        
        for (Project project : allProjects) {
            if (project.getTeammates() != null && project.getTeammates().contains(emailId)) {
                teammateProjects.put(project.getId(), project.getProjectName());
            }
        }
        
        return teammateProjects;
    }

    public Task addTaskToProject(String projectId, String creatorEmail, AddTaskToProjectRequest addTaskRequest) {
        Project project = getProject(projectId);
        if (project == null) {
            log.error("Project not found with id: {}", projectId);
            return null;
        }

        User creator = userService.getUser(creatorEmail);
        if (creator == null) {
            log.error("Creator user not found with email: {}", creatorEmail);
            return null;
        }

        Task task = new Task();
        task.setTaskName(addTaskRequest.getTaskName());
        task.setDescription(addTaskRequest.getDescription());
        task.setStartDate(addTaskRequest.getStartDate() != null ? addTaskRequest.getStartDate() : addTaskRequest.getDate());
        task.setEndDate(addTaskRequest.getEndDate());
        task.setDate(addTaskRequest.getDate());
        task.setPriority(addTaskRequest.getPriority());
        task.setStatus(addTaskRequest.getStatus());
        task.setCompleted(false);
        task.setCreatedId(creator.getId());
        String group = addTaskRequest.getGroup();
        if (group == null || group.trim().isEmpty()) {
            group = "To do"; // Default group
        }
        task.setGroup(group);
        task.setProjectIds(Collections.singletonList(projectId));

        List<String> assignIds = new ArrayList<>();
        if (addTaskRequest.getAssignId() != null) {
            for (String assigneeEmail : addTaskRequest.getAssignId()) {
                User assignee = userService.getUser(assigneeEmail);
                if (assignee != null) {
                    assignIds.add(assignee.getId());
                    if (!assignee.getId().equals(creator.getId())) {
                        inboxService.sendTaskDetails(task, creatorEmail, assigneeEmail);
                    }
                } else {
                    log.warn("Assignee user not found for email: {}", assigneeEmail);
                }
            }
        }
        task.setAssignId(assignIds);

        Task savedTask = taskService.save(task);
        log.info("Saved new task with id: {}", savedTask.getId());

        //project.getTasks().computeIfAbsent(group, k -> new ArrayList<>()).add(savedTask.getId());

        save(project);
        log.info("Updated project {} with new task {}", projectId, savedTask.getId());

        return savedTask;
    }

    public boolean addProjectSection(String projectId, SectionData sectionData) {
        if (sectionData == null || sectionData.getSectionName() == null || sectionData.getSectionName().isBlank())
            return false;

        Project project = getProject(projectId);
        if (project == null) return false;

        // Optional: prevent duplicate section names
        for (Section existing : project.getSection()) {
            if (existing.getSectionName().equalsIgnoreCase(sectionData.getSectionName())) {
                return false; // Don't add duplicate section names
            }
        }

        Section section = new Section();
        section.setId(UUID.randomUUID().toString()); // Ensure unique ID
        section.setSectionName(sectionData.getSectionName());

        project.getSection().add(section);
        save(project);

        System.out.println("Section added: " + section.getId() + " → " + section.getSectionName());

        return true;
    }

    public List<Project> getProjects() {
        return projectRepo.findAll();
    }

    public Boolean deleteProject(String projectId) {
        Project project = getProject(projectId);
        if (project == null) {
            log.error("Project not found with id: {}", projectId);
            return false;
        }
        projectRepo.delete(project);
        log.info("Deleted project with id: {}", projectId);

        List<User> users = userService.getAllUsers();

        for (User user : users) {

            List<Project> myproject = user.getProjects();

            myproject.removeIf(p -> p.getId().equals(projectId));

            user.setProjects(myproject);
            userService.save(user);
        }

        return true;
    }

    public boolean deleteSection(String sectionId) {
        List<Project> projectList = getProjects();

        if (projectList.isEmpty()) return false;

        for (Project project : projectList) {
            List<Section> sections = project.getSection();

            Iterator<Section> iterator = sections.iterator();
            while (iterator.hasNext()) {
                Section section = iterator.next();

                if (section.getId().equals(sectionId)) {
                    List<Task> tasks = section.getTasks();

                    if (tasks != null && !tasks.isEmpty()) {
                        for (Task task : tasks) {
                            taskService.deleteTask(task.getId());
                        }
                        log.info("Section's Task Deleted with id: {}", sectionId);
                    }

                    iterator.remove();
                    log.info("Deleted Section with id: {}", sectionId);
                }
            }
            save(project);
        }
        return true;
    }

    public Map<String, Object> getDashboardStats(String projectId) {
        Map<String, Object> stats = new HashMap<>();
        List<Task> tasks = taskService.getTasksByProjectId(projectId);
        Project project = getProject(projectId);
        if (project == null) {
            stats.put("error", "Project not found");
            return stats;
        }
        int totalTasks = tasks.size();
        int totalCompletedTasks = 0;
        int totalIncompleteTasks = 0;
        int totalOverdueTasks = 0;
        Map<String, Integer> incompleteTasksBySection = new HashMap<>();
        Map<String, Integer> tasksByCompletionStatus = new HashMap<>();
        Map<String, Integer> upcomingTasksByAssignee = new HashMap<>();
        Map<String, Integer> completedTasksByDate = new HashMap<>();
        Map<String, Integer> totalTasksByDate = new HashMap<>();
        Date now = new Date();
        for (Section section : project.getSection()) {
            int incompleteCount = 0;
            if (section.getTasks() != null) {
                for (Task task : section.getTasks()) {
                    if (!task.isCompleted()) {
                        incompleteCount++;
                    }
                }
            }
            incompleteTasksBySection.put(section.getSectionName(), incompleteCount);
        }
        for (Task task : tasks) {
            boolean completed = task.isCompleted();
            if (completed) totalCompletedTasks++;
            else totalIncompleteTasks++;
            // Overdue: incomplete and due date before now
            if (!completed && task.getStartDate() != null && task.getStartDate().before(now)) {
                totalOverdueTasks++;
            }
            // Completion status
            String statusKey = completed ? "Completed" : "Incomplete";
            tasksByCompletionStatus.put(statusKey, tasksByCompletionStatus.getOrDefault(statusKey, 0) + 1);
            // Upcoming by assignee (only for incomplete tasks)
            if (!completed && task.getAssignId() != null) {
                for (String userId : task.getAssignId()) {
                    upcomingTasksByAssignee.put(userId, upcomingTasksByAssignee.getOrDefault(userId, 0) + 1);
                }
            }
            // Completion over time (by date, formatted as yyyy-MM-dd)
            if (task.getStartDate() != null) {
                String dateKey = new java.text.SimpleDateFormat("yyyy-MM-dd").format(task.getStartDate());
                totalTasksByDate.put(dateKey, totalTasksByDate.getOrDefault(dateKey, 0) + 1);
                if (completed) {
                    completedTasksByDate.put(dateKey, completedTasksByDate.getOrDefault(dateKey, 0) + 1);
                }
            }
        }
        // Build completion over time list
        List<Map<String, Object>> taskCompletionOverTime = new ArrayList<>();
        for (String date : totalTasksByDate.keySet()) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("date", date);
            entry.put("total", totalTasksByDate.get(date));
            entry.put("completed", completedTasksByDate.getOrDefault(date, 0));
            taskCompletionOverTime.add(entry);
        }
        stats.put("totalCompletedTasks", totalCompletedTasks);
        stats.put("totalIncompleteTasks", totalIncompleteTasks);
        stats.put("totalOverdueTasks", totalOverdueTasks);
        stats.put("totalTasks", totalTasks);
        stats.put("incompleteTasksBySection", incompleteTasksBySection);
        stats.put("tasksByCompletionStatus", tasksByCompletionStatus);
        stats.put("upcomingTasksByAssignee", upcomingTasksByAssignee);
        stats.put("taskCompletionOverTime", taskCompletionOverTime);
        return stats;
    }

    public boolean updateProject(ProjectOverview projectOverview, String projectId) {

        Project project = getProject(projectId);

        if(project == null){
            log.error("Project not found");
            return false;
        }

        if(!projectOverview.getProjectDescription().isEmpty()) project.setProjectDescription(projectOverview.getProjectDescription());
        if(projectOverview.getStartDate() != null) project.setStartDate(projectOverview.getStartDate());
        if(projectOverview.getEndDate() != null) project.setEndDate(projectOverview.getEndDate());

        save(project);
        log.info("Project updated");
        return true;
    }

    public HashMap<String,Object> getProjectOverview(String projectId) {
        Project project = getProject(projectId);

        if(project == null){
            log.error("Project not found");
            return null;
        }

        HashMap<String,Object> data = new HashMap<>();

        data.put("Project description", project.getProjectDescription());
        String projectOwner = userService.getUser(project.getUserEmailid()).getFullName();
        data.put("Project Roles", projectOwner);
        data.put("Project start date",project.getStartDate());
        data.put("Project end date",project.getEndDate());

        log.info("Project overview Retrieved");
        return data;

    }
}
