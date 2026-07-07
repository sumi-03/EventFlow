package com.example.eventflow.domain.auth.service;

import com.example.eventflow.domain.auth.dto.*;
import com.example.eventflow.domain.auth.entity.RefreshToken;
import com.example.eventflow.domain.auth.repository.RefreshTokenRepository;
import com.example.eventflow.domain.user.entity.User;
import com.example.eventflow.domain.user.entity.UserRole;
import com.example.eventflow.domain.user.repository.UserRepository;
import com.example.eventflow.global.exception.BusinessException;
import com.example.eventflow.global.jwt.JwtTokenProvider;
import com.example.eventflow.global.payload.status.ErrorStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorStatus.EMAIL_DUPLICATED);
        }
        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                UserRole.USER
        );
        return SignupResponse.from(userRepository.save(user));
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorStatus.LOGIN_FAILED));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorStatus.LOGIN_FAILED);
        }
        return issueTokens(user);
    }

    /**
     * Refresh Token 회전(RTR): 유효성 + DB 존재를 검증한 뒤 기존 토큰을 삭제하고
     * Access/Refresh 를 모두 새로 발급한다.
     */
    @Transactional
    public TokenResponse reissue(ReissueRequest request) {
        String refreshToken = request.refreshToken();
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorStatus.INVALID_REFRESH_TOKEN);
        }
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorStatus.REFRESH_TOKEN_NOT_FOUND));

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorStatus.USER_NOT_FOUND));

        refreshTokenRepository.delete(stored);
        return issueTokens(user);
    }

    @Transactional
    public void logout(ReissueRequest request) {
        refreshTokenRepository.deleteByToken(request.refreshToken());
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = tokenProvider.createAccessToken(user);
        String refreshToken = tokenProvider.createRefreshToken(user);
        refreshTokenRepository.save(new RefreshToken(
                user.getId(), refreshToken, tokenProvider.getExpiration(refreshToken)));
        return TokenResponse.of(accessToken, refreshToken);
    }
}
