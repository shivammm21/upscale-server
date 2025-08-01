package com.upscale.upscale.service.project;

import com.upscale.upscale.dto.project.InboxData;
import com.upscale.upscale.entity.project.*;
import com.upscale.upscale.entity.user.User;
import com.upscale.upscale.repository.InboxRepo;
import com.upscale.upscale.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class InboxService {

    @Autowired
    private InboxRepo inboxRepo;

    public void saveInbox(Inbox inbox){
        inboxRepo.save(inbox);
    }

    public void updateInbox(String emailId, Inbox inbox){
        inboxRepo.save(inbox);
    }

    public void sendInviteInbox(String senderEmailId, String receiverEmailId, People people){
        Inbox inbox = new Inbox();

        inbox.setSenderId(senderEmailId);
        inbox.setReceiverId(receiverEmailId);

        String context = "You have invite for the projects"+people.getProjectsName();

        inbox.setContent(context);

        saveInbox(inbox);
    }

    @Autowired
    @Lazy
    private UserService userService;

    public void sendProjectInvite(String senderEmailId, String receiverEmailId, Project project, User user) {
        Inbox inbox = new Inbox();
        inbox.setSenderId(senderEmailId);
        inbox.setReceiverId(receiverEmailId);
        
        String context = String.format("You have been invited to join the project '%s' in workspace '%s'", 
            project.getProjectName(), 
            project.getWorkspace());
        
        inbox.setContent(context);
        saveInbox(inbox);

        //user.getProjects().add(project.getId());

        List<String> projectIds = user.getProjects();
        projectIds.add(project.getId());

        user.setProjects(projectIds);

        userService.save(user);

    }

    public List<InboxData> getInbox(String emailId){
        List<Inbox> inboxes = inboxRepo.findByReceiverId(emailId);

        List<InboxData> inboxDataList = new ArrayList<>();

        if(inboxes != null && !inboxes.isEmpty()){
            for (Inbox inbox : inboxes) {
                InboxData inboxData = new InboxData();

                inboxData.setSenderId(inbox.getSenderId());
                inboxData.setReceiverId(inbox.getReceiverId());
                inboxData.setContent(inbox.getContent());

                inboxDataList.add(inboxData);
            }
            return inboxDataList;
        }
        return new ArrayList<>();
    }

    public void sendTaskDetails(Task task, String senderEmailId, String receiverEmailId){
        Inbox inbox = new Inbox();

        inbox.setSenderId(senderEmailId);
        inbox.setReceiverId(receiverEmailId);

        String context = "You have given a task "+task.getTaskName();

        inbox.setContent(context);

        saveInbox(inbox);
    }

    public void sendProjectMessage(Message message, String senderEmailId, String receiverEmailId){

        Inbox inbox = new Inbox();

        inbox.setSenderId(senderEmailId);
        inbox.setReceiverId(receiverEmailId);

        inbox.setContent(message.getBody());

        saveInbox(inbox);
    }
}
