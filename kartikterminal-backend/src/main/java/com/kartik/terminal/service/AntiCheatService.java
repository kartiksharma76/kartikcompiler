package com.kartik.terminal.service;

import com.kartik.terminal.entity.AntiCheatLog;
import com.kartik.terminal.entity.Exam;
import com.kartik.terminal.entity.User;
import com.kartik.terminal.repository.AntiCheatLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AntiCheatService {
    
    private final AntiCheatLogRepository antiCheatRepo;

    public void logCheatEvent(Exam exam, User student, AntiCheatLog.EventType eventType, String details) {
        AntiCheatLog log = AntiCheatLog.builder()
            .exam(exam)
            .student(student)
            .eventType(eventType)
            .eventDetails(details)
            .build();
            
        antiCheatRepo.save(log);
    }
}
