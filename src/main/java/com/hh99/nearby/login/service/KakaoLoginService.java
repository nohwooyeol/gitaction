package com.hh99.nearby.login.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hh99.nearby.entity.Member;
import com.hh99.nearby.login.dto.KakaoRequestDto;
import com.hh99.nearby.signup.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class KakaoLoginService {

    private final PasswordEncoder passwordEncoder;

    private final MemberRepository memberRepository;

    @Value("${kakao.client.id}")
    String restapikey;

    @Value("${kakao.redirect.url}")
    String url;


    public KakaoRequestDto kakaoLogin(String code)throws JsonProcessingException {

        // 1. "인가 코드"로 "액세스 토큰" 요청
        String accessToken = getAccessToken(code);

        // 2. 토큰으로 카카오 API 호출
        KakaoRequestDto kakaoUserInfo = getKakaoUserInfo(accessToken); //엑세스 토큰값으로 유저 정보 받아오기!

//         3. 카카오ID로 회원가입 처리
        Member kakaoUser = registerKakaoUserIfNeed(kakaoUserInfo); //담아온 카카오데이터로 로그인 처리


        return kakaoUserInfo; //kakaouser로 리턴~
    }

    // 1. "인가 코드"로 "액세스 토큰" 요청
    private String getAccessToken(String code) throws JsonProcessingException { //인가코드 파라미터
        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders(); // 헤더를 생성
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
        //헤더에 값을 넣어줌

        // HTTP Body 생성
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>(); //바디는 키:벨류로 데이터를 보내서 MultivalueMap 사용
        body.add("grant_type", "authorization_code"); //grant_type
        body.add("client_id", restapikey); //내 restapi 키
        body.add("redirect_uri", url); //리다이렉트 Url
        body.add("code", code); //카카오로 받는 인가코드

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(body, headers);
        RestTemplate rt = new RestTemplate();//서버에게 요청을 보냄
        ResponseEntity<String> response = rt.exchange(
                "https://kauth.kakao.com/oauth/token",
                HttpMethod.POST, //매서드는 포스트타입
                kakaoTokenRequest, // 카카오서버로 보낼 httpEntity
                String.class //스트링 클래스로
        ); //카카오 데이터 받음

        // HTTP 응답 (JSON) -> 액세스 토큰 파싱
        String responseBody = response.getBody(); //바디 부분
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody); //Json형태를 객체로 바꾸기
        return jsonNode.get("access_token").asText(); //엑세스토큰 받은거 넘기기
    }

    // 2. 토큰으로 카카오 API 호출
    private KakaoRequestDto getKakaoUserInfo(String accessToken) throws JsonProcessingException {
        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders(); // 헤더넣는 객체
        headers.add("Authorization", "Bearer " + accessToken); //엑세스 토큰 값
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8"); //타입

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> kakaoUserInfoRequest = new HttpEntity<>(headers);
        RestTemplate rt = new RestTemplate();//서버에게 요청을 보냄
        ResponseEntity<String> response = rt.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.POST,  //매서드는 포스트타입
                kakaoUserInfoRequest, // 카카오서버로 보낼 httpEntity
                String.class //스트링 클래스로
        );

        // responseBody에 있는 정보를 꺼냄
        String responseBody = response.getBody(); //바디에 정보를 꺼냄
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody); //Json형태를 객체로 바꾸기

        Long id = jsonNode.get("id").asLong(); // 아이디
        //String email = jsonNode.get("kakao_account").get("email").asText(); //이메일
        String nickname = jsonNode.get("properties") // 닉네임
                .get("nickname").asText();
        String profileImg = jsonNode.get("properties").get("profile_image").asText(); //프로필url

        return new KakaoRequestDto(id, nickname,profileImg); //Dto 에 담아서 리턴
    }

    // 3. 카카오ID로 회원가입 처리
    private Member registerKakaoUserIfNeed (KakaoRequestDto kakaoUserInfo) {

        Long kakaoid = kakaoUserInfo.getKakaoid();
        String nickname = kakaoUserInfo.getNickname(); //닉네임값
        String profileImg = kakaoUserInfo.getProfileImg();

        Member kakaoUser = memberRepository.findByNickname(nickname)  //카카오 닉네임이 있는지 확인
                .orElse(null);

        if (kakaoUser == null) { //db에 이메일이 없다면
            // 회원가입
            // password: random UUID
           // String password = UUID.randomUUID().toString(); //이건 없어도 됨
            // String encodedPassword = passwordEncoder.encode(password);//패스워드 암호화

            String profile = "https://ossack.s3.ap-northeast-2.amazonaws.com/basicprofile.png"; //프로필 이미지

//            kakaoUser = new User(kakaoEmail, nickname, profile, encodedPassword); // 카카오 유저를 담을 객체
            kakaoUser = Member.builder()
                    .email(null)
                    .nickname(nickname)
                    .password(null)
                    .profileImg(profileImg)
                    .emailCheck(true)
                    .kakaoId(kakaoid)
                    .build();
            memberRepository.save(kakaoUser); // db에 저장

        }
        return kakaoUser; //유저정보 리턴
    }
}
