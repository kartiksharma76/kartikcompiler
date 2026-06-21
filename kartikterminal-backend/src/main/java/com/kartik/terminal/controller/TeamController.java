package com.kartik.terminal.controller;

import com.kartik.terminal.entity.Team;
import com.kartik.terminal.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<Team> createTeam(@RequestBody Team team) {
        return ResponseEntity.ok(teamService.createTeam(team));
    }

    @GetMapping("/{code}")
    public ResponseEntity<Team> getTeam(@PathVariable String code) {
        return ResponseEntity.ok(teamService.getTeamByCode(code));
    }
}
