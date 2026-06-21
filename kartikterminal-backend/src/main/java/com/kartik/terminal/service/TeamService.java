package com.kartik.terminal.service;

import com.kartik.terminal.entity.Team;
import com.kartik.terminal.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeamService {
    private final TeamRepository teamRepository;

    public Team createTeam(Team team) {
        return teamRepository.save(team);
    }

    public Team getTeamByCode(String code) {
        return teamRepository.findByTeamCode(code)
            .orElseThrow(() -> new RuntimeException("Team not found"));
    }
}
