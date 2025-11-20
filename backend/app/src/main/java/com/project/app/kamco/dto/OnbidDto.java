package com.project.app.kamco.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data                   // getter, setter, equals, hashCode, toString 자동 생성
@NoArgsConstructor      // 파라미터 없는 기본 생성자
@AllArgsConstructor     // 모든 필드를 파라미터로 받는 생성자
@Builder                // 빌더 패턴 구현
public class OnbidDto {
	private Long id;	// 순번 (RNUM)
    private String rnum;
    private String plnmNo;    // 공매계획번호 (PLNM_NO)
    private String pbctNo;    // 공고번호 (PBCT_NO)
    private String pbctCdtnNo;    // 공고조건번호 (PBCT_CDTN_NO)
    private String cltrNo;    // 물건번호 (CLTR_NO)
    private String cltrHstrNo;    // 물건이력번호 (CLTR_HSTR_NO) - Primary Key 후보
    private String scrnGrpCd;    // 화면그룹코드 (SCRN_GRP_CD)
    private String ctgrFullNm;    // 카테고리전체명 (CTGR_FULL_NM)
    private String bidMnmtNo;    // 입찰관리번호 (BID_MNMT_NO)
    private String cltrNm;    // 물건명 (CLTR_NM)
    private String cltrMnmtNo;    // 물건관리번호 (CLTR_MNMT_NO)
    private String ldnmAdrs;    // 지번주소 (LDNM_ADRS)
    private String nmrdAdrs;    // 도로명주소 (NMRD_ADRS)
    private String ldnmPnu;    // 지번고유번호 (LDNM_PNU)
    private String dpslMtdCd;    // 처분방법코드 (DPSL_MTD_CD)
    private String dpslMtdNm;    // 처분방법명 (DPSL_MTD_NM)
    private String bidMtdNm;    // 입찰방법명 (BID_MTD_NM)
    private String minBidPrc;    // 최저입찰가 (MIN_BID_PRC) - 금액이므로 BigDecimal 권장
    private String apslAsesAvgAmt;    // 감정평가평균금액 (APSL_ASES_AVG_AMT) - 금액이므로 BigDecimal 권장
    private String feeRate;    // 수수료율 (FEE_RATE)
    private String pbctBegnDtm;    // 공고시작일시 (PBCT_BEGN_DTM) - YYYYMMDDHHMISS 문자열이므로 String 또는 LocalDateTime
    private String pbctClsDtm;
    private String pbctCltrStatNm;    // 공고물건상태명 (PBCT_CLTR_STAT_NM)
    private String uscbdCnt;    // 유찰회수 (USCBD_CNT)
    private String iqryCnt;    // 조회건수 (IQRY_CNT)
    private String goodsNm;    // 물품명세 (GOODS_NM)
    private String manf;    // 제조사 (MANF)
    private String mdl;    // 모델 (MDL)
    private String nrgt;    // 배기량 (NRGT)
    private String grbx;    // 변속기 (GRBX)
    private String endpc;    // 최종출력 (ENDPC)
    private String vhclMlE;     // 주행거리 (VHCL_MLGE)
    private String fuel;    // 연료 (FUEL)
    private String scrtNm;    // 보증서명 (SCRT_NM)
    private String tpbz;    // 업종 (TPBZ)
    private String itmNm;    // 품목명 (ITM_NM)
    private String mmbRgtNm;		// 회원권종명 (MMB_RGT_NM)
    private String cltrImgFiles;	// 물건이미지파일경로 (CLTR_IMG_FILES)
}
