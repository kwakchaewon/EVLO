package com.evlo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * /favicon.ico 요청을 처리하여 404/500 로그를 방지합니다.
 * 아이콘 파일이 없으므로 204 No Content를 반환합니다.
 */
@Controller
public class FaviconController {

    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent().build();
    }
}
