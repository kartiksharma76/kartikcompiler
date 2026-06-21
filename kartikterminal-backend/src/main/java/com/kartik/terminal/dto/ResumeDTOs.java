package com.kartik.terminal.dto;

import lombok.Data;
import java.util.List;

public class ResumeDTOs {

    @Data
    public static class ResumeRequest {
        private String fullName;
        private String email;
        private String phone;
        private String linkedinProfile;
        private String githubProfile;
        private String summary;
        private String skills;
        private List<ExperienceRequest> experiences;
        private List<EducationRequest> educations;
        private List<ProjectRequest> projects;
    }

    @Data
    public static class ExperienceRequest {
        private String company;
        private String role;
        private String startDate;
        private String endDate;
        private String description;
    }

    @Data
    public static class EducationRequest {
        private String institution;
        private String degree;
        private String fieldOfStudy;
        private String startDate;
        private String endDate;
        private String grade;
    }

    @Data
    public static class ProjectRequest {
        private String title;
        private String projectUrl;
        private String description;
    }
}
