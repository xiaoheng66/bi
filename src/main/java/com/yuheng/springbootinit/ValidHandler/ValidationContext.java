package com.yuheng.springbootinit.ValidHandler;

import org.springframework.web.multipart.MultipartFile;

public class ValidationContext {
    private final String name;
    private final String goal;
    private final MultipartFile multipartFile;

    public ValidationContext(String name, String goal, MultipartFile multipartFile) {
        this.name = name;
        this.goal = goal;
        this.multipartFile = multipartFile;
    }

    public String getName() {
        return name;
    }

    public String getGoal() {
        return goal;
    }

    public MultipartFile getMultipartFile() {
        return multipartFile;
    }
}
