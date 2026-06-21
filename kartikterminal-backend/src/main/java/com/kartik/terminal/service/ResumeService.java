package com.kartik.terminal.service;

import com.kartik.terminal.dto.ResumeDTOs.*;
import com.kartik.terminal.entity.*;
import com.kartik.terminal.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeService {
    
    private final AuthService authService;
    private final ResumeRepository resumeRepository;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final ProjectRepository projectRepository;
    private final AIService aiService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ResumeRequest saveOrUpdateResume(ResumeRequest request) {
        User user = authService.getCurrentUser();
        
        Optional<Resume> existingResumeOpt = resumeRepository.findByUserId(user.getId());
        Resume resume;
        boolean isNew = false;
        
        if (existingResumeOpt.isPresent()) {
            resume = existingResumeOpt.get();
            // Clear existing linked entities for pure replacement
            experienceRepository.deleteAll(experienceRepository.findByResumeId(resume.getId()));
            educationRepository.deleteAll(educationRepository.findByResumeId(resume.getId()));
            projectRepository.deleteAll(projectRepository.findByResumeId(resume.getId()));
        } else {
            resume = new Resume();
            resume.setUser(user);
            isNew = true;
        }
        
        resume.setFullName(request.getFullName());
        resume.setEmail(request.getEmail());
        resume.setPhone(request.getPhone());
        resume.setLinkedinProfile(request.getLinkedinProfile());
        resume.setGithubProfile(request.getGithubProfile());
        resume.setSummary(request.getSummary());
        resume.setSkills(request.getSkills());
        
        resume = resumeRepository.save(resume);
        
        // Save experiences
        if (request.getExperiences() != null) {
            for (ExperienceRequest exReq : request.getExperiences()) {
                Experience exp = new Experience();
                exp.setResume(resume);
                exp.setCompany(exReq.getCompany());
                exp.setRole(exReq.getRole());
                exp.setStartDate(exReq.getStartDate());
                exp.setEndDate(exReq.getEndDate());
                exp.setDescription(exReq.getDescription());
                experienceRepository.save(exp);
            }
        }
        
        // Save education
        if (request.getEducations() != null) {
            for (EducationRequest edReq : request.getEducations()) {
                Education edu = new Education();
                edu.setResume(resume);
                edu.setInstitution(edReq.getInstitution());
                edu.setDegree(edReq.getDegree());
                edu.setFieldOfStudy(edReq.getFieldOfStudy());
                edu.setStartDate(edReq.getStartDate());
                edu.setEndDate(edReq.getEndDate());
                edu.setGrade(edReq.getGrade());
                educationRepository.save(edu);
            }
        }
        
        // Save projects
        if (request.getProjects() != null) {
            for (ProjectRequest prReq : request.getProjects()) {
                Project proj = new Project();
                proj.setResume(resume);
                proj.setTitle(prReq.getTitle());
                proj.setProjectUrl(prReq.getProjectUrl());
                proj.setDescription(prReq.getDescription());
                projectRepository.save(proj);
            }
        }
        
        return getMyResume();
    }
    
    @Transactional(readOnly = true)
    public ResumeRequest getMyResume() {
        User user = authService.getCurrentUser();
        Optional<Resume> resumeOpt = resumeRepository.findByUserId(user.getId());
        
        if (resumeOpt.isEmpty()) {
            return null; // Handle smoothly in controller (e.g. 404)
        }
        
        Resume resume = resumeOpt.get();
        ResumeRequest res = new ResumeRequest();
        res.setFullName(resume.getFullName());
        res.setEmail(resume.getEmail());
        res.setPhone(resume.getPhone());
        res.setLinkedinProfile(resume.getLinkedinProfile());
        res.setGithubProfile(resume.getGithubProfile());
        res.setSummary(resume.getSummary());
        res.setSkills(resume.getSkills());
        
        List<ExperienceRequest> exps = experienceRepository.findByResumeId(resume.getId()).stream().map(e -> {
            ExperienceRequest r = new ExperienceRequest();
            r.setCompany(e.getCompany());
            r.setRole(e.getRole());
            r.setStartDate(e.getStartDate());
            r.setEndDate(e.getEndDate());
            r.setDescription(e.getDescription());
            return r;
        }).collect(Collectors.toList());
        res.setExperiences(exps);
        
        List<EducationRequest> edus = educationRepository.findByResumeId(resume.getId()).stream().map(e -> {
            EducationRequest r = new EducationRequest();
            r.setInstitution(e.getInstitution());
            r.setDegree(e.getDegree());
            r.setFieldOfStudy(e.getFieldOfStudy());
            r.setStartDate(e.getStartDate());
            r.setEndDate(e.getEndDate());
            r.setGrade(e.getGrade());
            return r;
        }).collect(Collectors.toList());
        res.setEducations(edus);
        
        List<ProjectRequest> projs = projectRepository.findByResumeId(resume.getId()).stream().map(p -> {
            ProjectRequest r = new ProjectRequest();
            r.setTitle(p.getTitle());
            r.setProjectUrl(p.getProjectUrl());
            r.setDescription(p.getDescription());
            return r;
        }).collect(Collectors.toList());
        res.setProjects(projs);
        
        return res;
    }

    public ResumeRequest optimizeResumeWithAI(ResumeRequest request) {
        try {
            // Build experience detail text
            StringBuilder expBuilder = new StringBuilder();
            if (request.getExperiences() != null) {
                for (int i = 0; i < request.getExperiences().size(); i++) {
                    ExperienceRequest exp = request.getExperiences().get(i);
                    expBuilder.append(String.format("Experience %d:\n- Company: %s\n- Role: %s\n- Description: %s\n\n",
                        i + 1, exp.getCompany(), exp.getRole(), exp.getDescription()));
                }
            }

            // Build project detail text
            StringBuilder projBuilder = new StringBuilder();
            if (request.getProjects() != null) {
                for (int i = 0; i < request.getProjects().size(); i++) {
                    ProjectRequest proj = request.getProjects().get(i);
                    projBuilder.append(String.format("Project %d:\n- Title: %s\n- Description: %s\n\n",
                        i + 1, proj.getTitle(), proj.getDescription()));
                }
            }

            String prompt = String.format(
                "You are an elite developer resume reviewer and professional writer.\n" +
                "Optimize the following software engineer's resume by improving their professional summary, project descriptions, and experience bullet points.\n" +
                "Make them sound extremely impactful, action-oriented (using strong action verbs like 'Engineered', 'Optimized', 'Architected'), and filled with relevant industry keywords (like high throughput, low latency, clean architecture, RESTful APIs).\n" +
                "Retain all core technical facts, company names, years, roles, and project titles exactly. Do not invent any new experiences or change existing dates/companies.\n\n" +
                "Input Resume:\n" +
                "Summary: %s\n" +
                "Skills: %s\n\n" +
                "Experiences:\n%s" +
                "Projects:\n%s" +
                "You MUST return your output exclusively as a valid JSON object matching this schema. Do not output markdown codeblocks, preambles, or postambles. The response MUST be parsable directly by Jackson ObjectMapper:\n" +
                "{\n" +
                "  \"summary\": \"fully rewritten, highly professional executive summary\",\n" +
                "  \"experiences\": [\n" +
                "    {\n" +
                "      \"company\": \"Company Name matching the input\",\n" +
                "      \"role\": \"Role Name matching the input\",\n" +
                "      \"description\": \"highly professional, action-oriented bullet points outlining accomplishments\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"projects\": [\n" +
                "    {\n" +
                "      \"title\": \"Project Title matching the input\",\n" +
                "      \"description\": \"highly professional, technical, and impact-driven description of the project\"\n" +
                "    }\n" +
                "  ]\n" +
                "}",
                request.getSummary() != null ? request.getSummary() : "",
                request.getSkills() != null ? request.getSkills() : "",
                expBuilder.toString(),
                projBuilder.toString()
            );

            String aiResponse = aiService.callNvidiaAI(prompt);
            // Clean markdown wrapper if LLM returned it
            if (aiResponse.contains("```json")) {
                aiResponse = aiResponse.substring(aiResponse.indexOf("```json") + 7);
                if (aiResponse.contains("```")) {
                    aiResponse = aiResponse.substring(0, aiResponse.indexOf("```"));
                }
            } else if (aiResponse.contains("```")) {
                aiResponse = aiResponse.substring(aiResponse.indexOf("```") + 3);
                if (aiResponse.contains("```")) {
                    aiResponse = aiResponse.substring(0, aiResponse.indexOf("```"));
                }
            }
            aiResponse = aiResponse.trim();

            com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(aiResponse);

            ResumeRequest optimized = new ResumeRequest();
            optimized.setFullName(request.getFullName());
            optimized.setEmail(request.getEmail());
            optimized.setPhone(request.getPhone());
            optimized.setLinkedinProfile(request.getLinkedinProfile());
            optimized.setGithubProfile(request.getGithubProfile());
            optimized.setSkills(request.getSkills());
            optimized.setEducations(request.getEducations());

            optimized.setSummary(rootNode.path("summary").asText(request.getSummary()));

            // Map experiences back
            if (request.getExperiences() != null) {
                java.util.ArrayList<ExperienceRequest> optimizedExps = new java.util.ArrayList<>();
                com.fasterxml.jackson.databind.JsonNode expsNode = rootNode.path("experiences");
                for (int i = 0; i < request.getExperiences().size(); i++) {
                    ExperienceRequest orig = request.getExperiences().get(i);
                    ExperienceRequest opt = new ExperienceRequest();
                    opt.setCompany(orig.getCompany());
                    opt.setRole(orig.getRole());
                    opt.setStartDate(orig.getStartDate());
                    opt.setEndDate(orig.getEndDate());
                    
                    // Match by company/role in the AI response
                    String desc = orig.getDescription();
                    if (expsNode.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode node : expsNode) {
                            String nodeComp = node.path("company").asText("");
                            if (nodeComp.equalsIgnoreCase(orig.getCompany())) {
                                desc = node.path("description").asText(desc);
                                break;
                            }
                        }
                    }
                    opt.setDescription(desc);
                    optimizedExps.add(opt);
                }
                optimized.setExperiences(optimizedExps);
            }

            // Map projects back
            if (request.getProjects() != null) {
                java.util.ArrayList<ProjectRequest> optimizedProjs = new java.util.ArrayList<>();
                com.fasterxml.jackson.databind.JsonNode projsNode = rootNode.path("projects");
                for (int i = 0; i < request.getProjects().size(); i++) {
                    ProjectRequest orig = request.getProjects().get(i);
                    ProjectRequest opt = new ProjectRequest();
                    opt.setTitle(orig.getTitle());
                    opt.setProjectUrl(orig.getProjectUrl());
                    
                    // Match by title in the AI response
                    String desc = orig.getDescription();
                    if (projsNode.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode node : projsNode) {
                            String nodeTitle = node.path("title").asText("");
                            if (nodeTitle.equalsIgnoreCase(orig.getTitle())) {
                                desc = node.path("description").asText(desc);
                                break;
                            }
                        }
                    }
                    opt.setDescription(desc);
                    optimizedProjs.add(opt);
                }
                optimized.setProjects(optimizedProjs);
            }

            return optimized;
        } catch (Exception e) {
            log.error("AI Resume Optimization failed, returning original resume request", e);
            return request;
        }
    }
}
