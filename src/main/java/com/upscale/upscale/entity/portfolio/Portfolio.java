package com.upscale.upscale.entity.portfolio;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "portfolio")
public class Portfolio {
    @Id
    private String id;
    private String ownerId;
    private String portfolioName;
    private String privacy;
    private String defaultView;
    private List<String> teammates = new ArrayList<>();

}
