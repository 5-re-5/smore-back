package org.oreo.smore.domain.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.oreo.smore.domain.user.dto.request.UserUpdateRequest;
import org.oreo.smore.domain.user.dto.response.UserInfoResponse;
import org.oreo.smore.domain.user.dto.response.UserUpdateResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PatchMapping(value = "/v1/users/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserUpdateResponse> updateUser(
            @PathVariable Long userId,
            @Valid @ModelAttribute UserUpdateRequest userUpdateRequest,
            Authentication authentication,
            BindingResult bindingResult) throws Exception {

//        if (Long.parseLong(authentication.getPrincipal().toString()) != userId) {
//            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // userId가 다르면 403
//        }
        if (authentication != null && Long.parseLong(authentication.getPrincipal().toString()) != userId) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // userId가 다르면 403
        }

        if (bindingResult.hasErrors()) {
            String message = bindingResult.getAllErrors().get(0).getDefaultMessage();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); // 입력 값이 유효하지 않으면 400
        }

        if (userUpdateRequest.getProfileImage() != null && !userUpdateRequest.getProfileImage().isEmpty()) {
            if (userUpdateRequest.getProfileImage().getSize() > 100 * 1024 * 1024) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 크기는 최대 100MB까지 가능합니다.");
            }
            String contentType = userUpdateRequest.getProfileImage().getContentType();
            if (!contentType.startsWith("image/")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일만 업로드 가능합니다.");
            }
        }

        return ResponseEntity.ok(userService.updateUser(userId, userUpdateRequest));
    }

    @GetMapping("/v1/users/{userId}")
    public ResponseEntity<UserInfoResponse> getUserInfo(
            @PathVariable Long userId,
            Authentication authentication) {

//        if (Long.parseLong(authentication.getPrincipal().toString()) != userId) {
//            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // userId가 다르면 403
//        }
        if (authentication != null && Long.parseLong(authentication.getPrincipal().toString()) != userId) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // userId가 다르면 403
        }

        return ResponseEntity.ok(userService.getUserInfo(userId));
    }

}
