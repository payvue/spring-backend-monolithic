package com.payvue.app.presentation.rest.auth;

import com.payvue.app.application.auth.AuthUseCase;
import com.payvue.app.application.auth.recovery.FindIdCommand;
import com.payvue.app.application.auth.recovery.FindPasswordCommand;
import com.payvue.app.presentation.rest.ApiResponse;
import com.payvue.app.presentation.rest.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
class AccountRecoveryController implements AccountRecoveryControllerDocs {

    private final AuthUseCase authUseCase;

    @PostMapping("/find-id")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> requestFindId(
            @jakarta.validation.Valid @RequestBody FindIdRequest request
    ) {
        try {

            authUseCase.findId(new FindIdCommand(request.email()));

            return ResponseEntity.ok(ApiResponse.of(
                    ResponseCode.SUCCESS,
                    "가입하신 이메일로 아이디 정보가 전송되었습니다.",
                    null
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.of(ResponseCode.INVALID_REQUEST, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.of(ResponseCode.FAIL, "아이디 찾기 처리 중 오류가 발생했습니다.", null));
        }
    }


    @PostMapping("/reset-password")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> requestResetPassword(
            @jakarta.validation.Valid @RequestBody FindPasswordRequest request
    ) {
        try {

            authUseCase.findPassword(new FindPasswordCommand(request.username(), request.email()));

            return ResponseEntity.ok(ApiResponse.of(
                    ResponseCode.SUCCESS,
                    "비밀번호 재설정 링크가 이메일로 전송되었습니다.",
                    null
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.of(ResponseCode.INVALID_REQUEST, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.of(ResponseCode.FAIL, "비밀번호 재설정 요청 중 오류가 발생했습니다.", null));
        }
    }
}
