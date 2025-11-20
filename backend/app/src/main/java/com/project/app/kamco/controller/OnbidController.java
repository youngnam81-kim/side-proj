package com.project.app.kamco.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.app.kamco.service.OnbidApiService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag(name = "공매물건 관리", description = "온비드 공매물건 관련 API")
@RestController
@RequestMapping("/api/onbid")
public class OnbidController {

    private final OnbidApiService onbidApiService;

    public OnbidController(OnbidApiService onbidApiService) {
        this.onbidApiService = onbidApiService;
    }

    @Operation(summary = "온비드 공매 물건 목록 조회 API", description = "지역, 물건 등으로 필터링된 공매물건의 이력 목록을 반환합니다.")
    @GetMapping("/list") // 프론트엔드가 호출할 실제 엔드포인트
    public ResponseEntity<?> getOnbidList(
            @RequestParam(defaultValue = "10") int numOfRows,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "0001") String prptDvsnCd,
            @RequestParam(required = false) String sido,
            @RequestParam(required = false) String sgk,
            @RequestParam(required = false) String emd,
            @RequestParam(required = false) String cltrMnmtNo,
            @RequestParam(required = false) String cltrNm) {
            // TODO: 다른 검색 조건 (input text 필드)도 여기에 @RequestParam으로 추가

        try {
            Map<String, Object> result = onbidApiService.getOnbidAuctionList(
                    numOfRows, pageNo, prptDvsnCd, sido, sgk, emd, cltrMnmtNo, cltrNm);
            return ResponseEntity.ok(result);
        } catch (JsonProcessingException e) {
            return ResponseEntity.internalServerError().body("Failed to parse OpenAPI response: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error calling Onbid API: " + e.getMessage());
        }
    }
}