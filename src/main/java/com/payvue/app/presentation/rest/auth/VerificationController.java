package com.payvue.app.presentation.rest.auth;

import com.payvue.app.application.auth.VerificationUseCase;
import com.payvue.app.application.auth.recovery.FindIdResult;
import com.payvue.app.domain.auth.VerificationPurpose;
import com.payvue.app.presentation.rest.ApiResponse;
import com.payvue.app.presentation.rest.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
class VerificationController implements VerificationControllerDocs {

    private final VerificationUseCase verificationUseCase;

    @GetMapping("/verify")
    @Transactional
    public ResponseEntity<ApiResponse<?>> verify(
            @RequestParam String token,
            @RequestParam VerificationPurpose purpose
    ) {

        try {
            return switch (purpose) {
                case SIGNUP -> verifySignup(token);
                case FIND_ID -> verifyFindId(token);
                case RESET_PASSWORD -> verifyResetPassword(token);
            };
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.of(
                            ResponseCode.INVALID_REQUEST,
                            e.getMessage(),
                            null
                    ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.of(
                            ResponseCode.FAIL,
                            "이메일 인증 처리 중 오류가 발생했습니다.",
                            null
                    ));
        }
    }

    /**
     * 인증 이메일 재발송
     * POST /api/auth/resend-verification
     */
    @PostMapping("/resend-verification")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestParam String email) {
        try {
            verificationUseCase.resendSignupVerification(email);

            return ResponseEntity.ok(ApiResponse.of(
                    ResponseCode.SUCCESS,
                    "인증 이메일이 재발송되었습니다.",
                    null
            ));

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.of(
                            ResponseCode.INVALID_REQUEST,
                            e.getMessage(),
                            null
                    ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.of(
                            ResponseCode.FAIL,
                            "이메일 발송에 실패했습니다.",
                            null
                    ));
        }
    }

    /**
     * 회원가입 이메일 인증
     */
    private ResponseEntity<ApiResponse<?>> verifySignup(String token) {
        verificationUseCase.verifySignup(token);

        return ResponseEntity.ok(ApiResponse.of(
                ResponseCode.SUCCESS,
                "이메일 인증이 완료되었습니다.",
                null
        ));
    }

    /**
     * ID 찾기 인증
     */
    private ResponseEntity<ApiResponse<?>> verifyFindId(String token) {
        FindIdResult result = verificationUseCase.verifyFindId(token);

        return ResponseEntity.ok(ApiResponse.of(
                ResponseCode.SUCCESS,
                "아이디를 찾았습니다.",
                result
        ));
    }

    /**
     * 비밀번호 재설정 인증 (토큰 검증만)
     * 실제 비밀번호 변경은 별도 POST 요청
     */
    private ResponseEntity<ApiResponse<?>> verifyResetPassword(String token) {
        return ResponseEntity.ok(ApiResponse.of(
                ResponseCode.SUCCESS,
                "임시 비밀번호가 발급되었습니다.",
                verificationUseCase.verifyResetPassword(token)
        ));
    }

}
