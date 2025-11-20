package com.project.app.kamco.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.app.kamco.dto.KamcoMyDto;
import com.project.app.kamco.mapper.KamcoMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class KamcoService {

    private final KamcoMapper kamcoMapper;

    public KamcoService(KamcoMapper kamcoMapper) {
        this.kamcoMapper = kamcoMapper;
    }

    @Transactional
    public void modifyMyData(KamcoMyDto kamcoMyDto) {
        int affectedRows = 0;

        if (kamcoMyDto.getIsFavorite() != null) {
            affectedRows = kamcoMapper.insertUpdateFavorite(kamcoMyDto);
        } else if (kamcoMyDto.getIsBid() != null) {
            affectedRows = kamcoMapper.insertUpdateBid(kamcoMyDto);
        }
        
        if (affectedRows == 0) {
            throw new RuntimeException("데이터 저장에 실패했습니다.");
        }
    }

	public KamcoMyDto getMyDataStatus(KamcoMyDto kamcoMyDto) {
		return kamcoMapper.selectMyDataStatus(kamcoMyDto);
	}

	public List<KamcoMyDto> getMyList(KamcoMyDto kamcoMyDto) {
		return kamcoMapper.selectMyDataList(kamcoMyDto);
	}

}