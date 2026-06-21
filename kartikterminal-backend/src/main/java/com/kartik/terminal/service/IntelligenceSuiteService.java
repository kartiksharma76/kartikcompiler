package com.kartik.terminal.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IntelligenceSuiteService {

    private final AIService aiService;

    public String generateVisaIntelligence(String targetCountry, String skills) {
        String prompt = String.format(
            "Act as an Elite Global Immigration Consultant. Analyze visa sponsorship pathways, H-1B lottery success chances, " +
            "relocation checklist, and golden visa possibilities for the target country '%s' with skills '%s'.\n" +
            "Render the response in professional GFM (GitHub Flavored Markdown) with clean tables, structured headers, and clear step-by-step action plans.\n",
            targetCountry, skills
        );
        return aiService.callNvidiaAI(prompt);
    }

    public String generateMentorshipConnector(String role, String targetCompany, String skills) {
        String prompt = String.format(
            "Act as a professional Executive Tech Headhunter and Mentor Matcher. Provide strategies on how to find, " +
            "connect with, and secure high-value industry mentors for a '%s' role at '%s' with skills '%s'.\n" +
            "Provide two copy-paste ready, highly customized, non-generic cold messages/outreach templates (one for LinkedIn, one for Email).\n" +
            "Render the output in clean GFM Markdown with clear headers.\n",
            role, targetCompany, skills
        );
        return aiService.callNvidiaAI(prompt);
    }

    public String generateInterviewSimulator(String role, String topic) {
        String prompt = String.format(
            "Act as an AI Behavioral and Technical Interviewer. Generate a mock high-pressure interview context " +
            "for a '%s' role focusing specifically on '%s'.\n" +
            "Provide 3 behavioral questions (STAR format) and 2 deep-dive system design/technical questions. " +
            "Explain exactly what qualities the interviewer is looking for in each question.\n" +
            "Format the response beautifully in GFM Markdown.\n",
            role, topic
        );
        return aiService.callNvidiaAI(prompt);
    }

    public String generateTalentHeatmap(String techStack) {
        String prompt = String.format(
            "Act as a Principal Recruitment Market Analyst. Generate a detailed regional talent market report and salary heatmap " +
            "for the tech stack '%s' (e.g. key cities, active corporate hirers, compensation bands, remote ratios, and talent density).\n" +
            "Include a markdown comparison table contrasting the top 3 global hubs for this stack.\n" +
            "Format beautifully in GFM Markdown.\n",
            techStack
        );
        return aiService.callNvidiaAI(prompt);
    }

    public String generateProjectArchitect(String projectIdea, String stack) {
        String prompt = String.format(
            "Act as a Principal System Architect. Create a production-grade blueprint for the project idea: '%s' " +
            "using the tech stack '%s'.\n" +
            "Provide: \n" +
            "1. High-level architecture diagrams in text form (or Mermaid code block if applicable)\n" +
            "2. Complete Database Schema (SQL DDL or NoSQL models)\n" +
            "3. Microservices modular boundary structure and folder layout representation.\n" +
            "Format the response in rich, comprehensive GFM Markdown.\n",
            projectIdea, stack
        );
        return aiService.callNvidiaAI(prompt);
    }

    public String generateSecurityAudit(String codeSnippet) {
        String prompt = String.format(
            "Act as a Lead DevSecOps & Security Auditor. Analyze the following code snippet for potential performance bottlenecks, " +
            "edge cases, and OWASP Top 10 vulnerabilities:\n\n" +
            "```\n%s\n```\n\n" +
            "Provide a comprehensive security report detailing findings, severity ratings (High, Medium, Low), and a secure, " +
            "optimized, fully rewritten code snippet as a direct replacement.\n" +
            "Format beautifully in GFM Markdown with clear sections.\n",
            codeSnippet
        );
        return aiService.callNvidiaAI(prompt);
    }

    public String generateSkillGraph(String currentStack, String targetJob) {
        String prompt = String.format(
            "Act as an elite Tech Career Coach. Build a personalized, high-value learning roadmap / skills roadmap " +
            "to bridge the professional gap from current skill set '%s' to the target role '%s'.\n" +
            "Divide the roadmap into Weeks 1-4 (Foundations), Weeks 5-8 (Advanced & Projects), and Weeks 9-12 (Interview Prep & Portfolios).\n" +
            "Include free learning resources and actionable milestone recommendations.\n" +
            "Format beautifully in GFM Markdown.\n",
            currentStack, targetJob
        );
        return aiService.callNvidiaAI(prompt);
    }

    public String generateOpenSourceHub(String preferredLanguage, String difficulty) {
        String prompt = String.format(
            "Act as an Open Source Contributor Mentor. Provide a highly actionable strategy and step-by-step guidance " +
            "on how a developer comfortable with '%s' can find, assign, and contribute to '%s' level open source issues " +
            "on GitHub (e.g. search filters, etiquette, draft PR recommendations).\n" +
            "Draft a contribution checklist.\n" +
            "Format beautifully in GFM Markdown.\n",
            preferredLanguage, difficulty
        );
        return aiService.callNvidiaAI(prompt);
    }

    public String generateHackathonEventFinder(String techStack, String domain) {
        String prompt = String.format(
            "Act as a Hackathon Champion and Innovation Coach. Design a high-impact hackathon strategy and pitch " +
            "for a team focusing on the domain '%s' using the tech stack '%s'.\n" +
            "Provide:\n" +
            "1. Three highly innovative, award-winning project concepts.\n" +
            "2. A compelling 30-second elevator pitch script for team formation and investor pitching.\n" +
            "3. Collaborative roles breakdown.\n" +
            "Format beautifully in GFM Markdown.\n",
            domain, techStack
        );
        return aiService.callNvidiaAI(prompt);
    }

    public String generateCareerMultiplier(String currentStack) {
        String prompt = String.format(
            "Act as a Tech Futurist and Career Path Strategist. Predict high-growth tech industry demand trends and recommend " +
            "high-yield pivot roadmaps for a developer currently utilizing '%s'.\n" +
            "Predict the demand shifts over the next 2-5 years and identify adjacent, high-paying tech domains (e.g. AI Engineers, Rust backend, Web3, Distributed systems).\n" +
            "Format beautifully in GFM Markdown.\n",
            currentStack
        );
        return aiService.callNvidiaAI(prompt);
    }
}
