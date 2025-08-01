package com.upscale.upscale.repository;

import com.upscale.upscale.entity.workspace.Workspace;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WorkspaceRepo extends MongoRepository<Workspace, String> {

    Workspace findByUserId(String userId);
}
