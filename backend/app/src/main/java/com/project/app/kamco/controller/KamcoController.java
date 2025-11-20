package com.project.app.kamco.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.app.kamco.dto.KamcoMyDto;
import com.project.app.kamco.service.KamcoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Tag(name = "공매물건 관리", description = "온비드 공매물건 관련 API")
@RestController
@RequestMapping("/api/kamco")
public class KamcoController {

    private final KamcoService kamcoService;

    public KamcoController(KamcoService kamcoService) {
        this.kamcoService = kamcoService;
    }

    @Operation(summary = "온비드 공매 물건 등록 및 수정 API", description = "사용자의 즐겨찾기, 입찰 내용을 저장합니다.")
    @PostMapping("/modifyMyData")
    public ResponseEntity<KamcoMyDto> addFavorites(@RequestBody KamcoMyDto kamcoMyDto) {
        kamcoService.modifyMyData(kamcoMyDto);
        KamcoMyDto updatedData = kamcoService.getMyDataStatus(kamcoMyDto);
        return ResponseEntity.ok(updatedData);
    }
    
    @Operation(summary = "사용자 공매 물건 Data를 불러옵니다. API", description = "사용자 공매 물건  Data를 조회합니다.")
    @GetMapping("/getMyDataStatus") // 프론트엔드에서 호출할 경로: /kamco/myitem-status
    public ResponseEntity<KamcoMyDto> getMyDataStatus(KamcoMyDto kamcoMyDto){
        KamcoMyDto myDataStatus = kamcoService.getMyDataStatus(kamcoMyDto);
        if (myDataStatus == null) {
            myDataStatus = new KamcoMyDto();
        }
        return ResponseEntity.ok(myDataStatus);
    }
    
    @Operation(summary = "사용자 DATA 조회 API", description = "사용자의 물건 목록을 반환합니다.")
    @GetMapping("/getMyList")
    public ResponseEntity<List<KamcoMyDto>> getMyList(KamcoMyDto kamcoMyDto) {
        List<KamcoMyDto> result = kamcoService.getMyList(kamcoMyDto);
        return ResponseEntity.ok(result);
    }


}

