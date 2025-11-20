package com.project.app.kamco.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data                   // getter, setter, equals, hashCode, toString 자동 생성
@NoArgsConstructor      // 파라미터 없는 기본 생성자
@AllArgsConstructor     // 모든 필드를 파라미터로 받는 생성자
@Builder                // 빌더 패턴 구현
public class KamcoMyDto {
	private String userId;
	private String cltrMnmtNo;	//물건관리번호
	private String cltrHstrNo;	//물건이력번호
	private String ctgrFullNm;	//카테고리
	private String cltrNm;		//물건명
	private String pbctBegnDtm;	//공고시작일시
	private String pbctClsDtm;	//공고종료일시
	private String feeRate;	//수수료율
	private String isFavorite;
	private String isBid;
	private String bidAmount;
	private String regDate;
	private String modDate;
	private String selectGbn;
}
