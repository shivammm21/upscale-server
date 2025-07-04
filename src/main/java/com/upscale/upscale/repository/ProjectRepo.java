package com.upscale.upscale.repository;

import com.upscale.upscale.entity.project.Project;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProjectRepo extends MongoRepository<Project, String> {
    Project findByUserEmailid(String emailId);
}
