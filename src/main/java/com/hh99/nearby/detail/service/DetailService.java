package com.hh99.nearby.detail.service;

import com.hh99.nearby.detail.dto.DetailResponseDto;
import com.hh99.nearby.entity.Challenge;
import com.hh99.nearby.entity.Member;
import com.hh99.nearby.entity.MemberChallenge;
import com.hh99.nearby.signup.repository.ChallengeRepository;
import com.hh99.nearby.signup.repository.MemberChallengeRepository;
import com.hh99.nearby.signup.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class DetailService {
    private final ChallengeRepository challengeRepository;
    private final MemberRepository memberRepository;
    private final MemberChallengeRepository memberChallengeRepository;

    @Transactional
    public ResponseEntity<?> detailModal(@PathVariable Long id) {
        Challenge challenge = isPresentChallenge(id);
        if (challenge == null) {
            return ResponseEntity.badRequest().body(Map.of("msg", "잘못된 챌린지 번호"));
        }
        DetailResponseDto detailResponseDto = DetailResponseDto.builder()
                .title(challenge.getTitle())
                .challengeImg(challenge.getChallengeimg())
                .startDay(challenge.getStarttday())
                .startTime(challenge.getStarttime())
                .targetTime(challenge.getTargettime())
                .endTime(challenge.getEndtime())
                .limitPeople(challenge.getLimitpeople())
                .content(challenge.getContent())
                .notice(challenge.getNotice())
                .writer(challenge.getWriter().getNickname())
                .build();
        return ResponseEntity.ok().body(Map.of("detailModal", detailResponseDto, "msg", "상세모달 조회 완료"));
    }

    @Transactional(readOnly = true)
    public Challenge isPresentChallenge(Long id) {
        Optional<Challenge> optionalChallenge = challengeRepository.findById(id);
        return optionalChallenge.orElse(null);
    }

    //참여하기
    @Transactional
    public ResponseEntity<?> participateChallenge(@PathVariable Long id, @AuthenticationPrincipal UserDetails user) {

        Challenge challenge = isPresentChallenge(id);
        if (challenge == null) {
            return ResponseEntity.badRequest().body(Map.of("msg", "잘못된 챌린지 번호"));
        }
        Optional<Member> member = memberRepository.findByEmail(user.getUsername());

        MemberChallenge memberChallenge = MemberChallenge.builder()
                .challenge(challenge)
                .member(member.get())
                .build();
        memberChallengeRepository.save(memberChallenge);
        return ResponseEntity.ok().body(Map.of("msg", "참여하기 완료"));
    }
}

