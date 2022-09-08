package com.hh99.nearby.challenge.controller;

import com.hh99.nearby.challenge.dto.ChallengeRequestDto;
import com.hh99.nearby.challenge.service.ChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RequiredArgsConstructor
@RestController
public class ChallengeController {
    private final ChallengeService challengeService;
    @PostMapping("/api/challenge")
    public ResponseEntity<?> createChallenge(@RequestBody ChallengeRequestDto challengeRequestDto, @AuthenticationPrincipal UserDetails user) throws ParseException {
       return challengeService.createChallenge(challengeRequestDto, user);
    }


    @PutMapping("/api/challenge/{challenge_id}")
    public ResponseEntity<?> updateChallenge(@PathVariable Long challenge_id, @RequestBody ChallengeRequestDto challengeRequestDto, @AuthenticationPrincipal UserDetails user){
        return challengeService.updateChallenge(challenge_id,challengeRequestDto, user);
    }

    @DeleteMapping("/api/challenge/{challenge_id}")
    public ResponseEntity<?> deleteChallenge(@PathVariable Long challenge_id, @AuthenticationPrincipal UserDetails user){
        return challengeService.deleteChallenge(challenge_id, user);
    }

}
