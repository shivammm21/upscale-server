package com.upscale.upscale.dto.project;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PeopleInvite {

    private String receiverEmailId;
    private List<String> projectId = new ArrayList<>();

}
