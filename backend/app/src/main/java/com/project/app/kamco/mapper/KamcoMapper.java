package com.project.app.kamco.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.project.app.kamco.dto.KamcoMyDto;

@Mapper
public interface KamcoMapper {

	int insertUpdateFavorite(KamcoMyDto kamcoMyDto); // Y,N
	int insertUpdateBid(KamcoMyDto kamcoMyDto); // Y,N
	KamcoMyDto selectMyDataStatus(KamcoMyDto kamcoMyDto);
	List<KamcoMyDto> selectMyDataList(KamcoMyDto kamcoMyDto);
    
}