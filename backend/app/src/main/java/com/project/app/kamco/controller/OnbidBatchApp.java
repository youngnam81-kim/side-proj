package com.project.app.kamco.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@SpringBootApplication
@RestController
@RequestMapping("/api/onbid")
public class OnbidBatchApp implements CommandLineRunner {

    @Autowired
    private DataSource dataSource;

	@Value("${onbid.api.service-key}")
	private String onbidApiServiceKey;

    public static void main(String[] args) {
        SpringApplication.run(OnbidBatchApp.class, args);
    }

    @Override
    public void run(String... args) {
        System.out.println("✅ OnbidBatchApp 실행 완료. /fetchOnbidData 호출로 데이터 적재 가능.");
    }

    @GetMapping("/batch")
    public String fetchAndInsertData() {
    	
    	Instant start = Instant.now(); 
    	
        int totalCount = 0;
        try {
            // === 추가된 로직: 데이터 적재 전 테이블 초기화 ===
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                
                System.out.println("🗑️ 기존 데이터 삭제 시작...");
                int deletedRows = stmt.executeUpdate("DELETE FROM KAMCO_AUCTION_ITEMS");
                //conn.commit(); 
                System.out.println("🗑️ 기존 데이터 " + deletedRows + "건 삭제 완료 및 오토 커밋.");
            }
            // ===========================================

            int currentPage = 1;
            int totalPages = 10; // 테스트를 위해 1페이지만 가져오도록 제한

            do {
                System.out.println("📡 [페이지 " + currentPage + "] 데이터 수집 시작...");

                String xmlResponse = fetchXmlFromApi(currentPage);
                List<OnbidItem> list = parseXmlToList(xmlResponse);
                System.out.println("📦 [페이지 " + currentPage + "] 파싱된 건수: " + list.size());

                insertOnbidList(list);
                totalCount += list.size();

                Thread.sleep(1000); 
                currentPage++;
            } while (currentPage <= totalPages);

         // --- 종료 시간 측정 및 경과 시간 계산 ---
            Instant end = Instant.now();
            Duration timeElapsed = Duration.between(start, end); // 두 Instant 사이의 Duration 계산
            long seconds = timeElapsed.getSeconds();
            long millis = timeElapsed.toMillis() % 1000;
            // -------------------------------------

            return "✅ 총 " + totalCount + "건 DB 적재 완료 (1~" + totalPages + "페이지). 소요 시간: " + seconds + "." + String.format("%03d", millis) + "초";
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 오류 발생: " + e.getMessage();
        }
    }

    private String fetchXmlFromApi(int pageNo) throws Exception {
        String serviceKey = onbidApiServiceKey; 
        
        StringBuilder urlBuilder = new StringBuilder("http://openapi.onbid.co.kr/openapi/services/KamcoPblsalThingInquireSvc/getKamcoPbctCltrList");
        urlBuilder.append("?" + URLEncoder.encode("serviceKey","UTF-8") + "=" + URLEncoder.encode(serviceKey, "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("numOfRows","UTF-8") + "=" + URLEncoder.encode("10000", "UTF-8"));
        urlBuilder.append("&" + URLEncoder.encode("pageNo","UTF-8") + "=" + URLEncoder.encode(String.valueOf(pageNo), "UTF-8"));

        URL url = new URL(urlBuilder.toString());
        
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/xml");

        int responseCode = conn.getResponseCode();
        BufferedReader rd;
        if (responseCode >= 200 && responseCode <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            System.err.println("❌ OpenAPI 호출 에러 코드: " + responseCode);
            StringBuilder errorSb = new StringBuilder();
            String errorLine;
            while ((errorLine = rd.readLine()) != null) errorSb.append(errorLine);
            System.err.println("❌ OpenAPI 에러 메시지: " + errorSb.toString());
            throw new Exception("Failed to fetch XML from OpenAPI. Response Code: " + responseCode + ", Error: " + errorSb.toString());
        }

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) sb.append(line);
        rd.close();
        conn.disconnect();
        return sb.toString();
    }

    private List<OnbidItem> parseXmlToList(String xmlResponse) throws Exception {
        List<OnbidItem> list = new ArrayList<>();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new java.io.ByteArrayInputStream(xmlResponse.getBytes(StandardCharsets.UTF_8)));
        doc.getDocumentElement().normalize();

        NodeList responseNodes = doc.getElementsByTagName("response");
        if (responseNodes.getLength() == 0) {
            System.err.println("XML 응답에서 <response> 태그를 찾을 수 없습니다.");
            return list;
        }

        Element responseElement = (Element) responseNodes.item(0);
        NodeList bodyNodes = responseElement.getElementsByTagName("body");
        if (bodyNodes.getLength() == 0) {
            System.err.println("XML 응답에서 <body> 태그를 찾을 수 없습니다.");
            return list;
        }
        Element bodyElement = (Element) bodyNodes.item(0);

        NodeList itemsNodes = bodyElement.getElementsByTagName("items");
        if (itemsNodes.getLength() == 0) {
            System.err.println("XML 응답에서 <items> 태그를 찾을 수 없습니다.");
            return list;
        }
        Element itemsElement = (Element) itemsNodes.item(0);

        NodeList nList = itemsElement.getElementsByTagName("item");

        if (nList.getLength() == 0) {
             System.out.println("현재 페이지에 파싱할 <item> 요소가 없습니다.");
             return list;
        }

        for (int i = 0; i < nList.getLength(); i++) {
            Node nNode = nList.item(i);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) nNode;
                OnbidItem item = new OnbidItem();
                
                item.RNUM = getTagValue("RNUM", e);
                item.PLNM_NO = getTagValue("PLNM_NO", e);
                item.PBCT_NO = getTagValue("PBCT_NO", e);
                item.PBCT_CDTN_NO = getTagValue("PBCT_CDTN_NO", e);
                item.CLTR_NO = getTagValue("CLTR_NO", e);
                item.CLTR_HSTR_NO = getTagValue("CLTR_HSTR_NO", e);
                item.SCRN_GRP_CD = getTagValue("SCRN_GRP_CD", e);
                item.CTGR_FULL_NM = getTagValue("CTGR_FULL_NM", e);
                item.BID_MNMT_NO = getTagValue("BID_MNMT_NO", e);
                item.CLTR_NM = getTagValue("CLTR_NM", e);
                item.CLTR_MNMT_NO = getTagValue("CLTR_MNMT_NO", e);
                item.LDNM_ADRS = getTagValue("LDNM_ADRS", e);
                item.NMRD_ADRS = getTagValue("NMRD_ADRS", e);
                item.LDNM_PNU = getTagValue("LDNM_PNU", e);
                item.DPSL_MTD_CD = getTagValue("DPSL_MTD_CD", e);
                item.DPSL_MTD_NM = getTagValue("DPSL_MTD_NM", e);
                item.BID_MTD_NM = getTagValue("BID_MTD_NM", e);
                item.MIN_BID_PRC = getTagValue("MIN_BID_PRC", e);
                item.APSL_ASES_AVG_AMT = getTagValue("APSL_ASES_AVG_AMT", e);
                item.FEE_RATE = getTagValue("FEE_RATE", e);
                item.PBCT_BEGN_DTM = getTagValue("PBCT_BEGN_DTM", e);
                item.PBCT_CLS_DTM = getTagValue("PBCT_CLS_DTM", e);
                item.PBCT_CLTR_STAT_NM = getTagValue("PBCT_CLTR_STAT_NM", e);
                item.USCBD_CNT = getTagValue("USCBD_CNT", e);
                item.IQRY_CNT = getTagValue("IQRY_CNT", e);
                item.GOODS_NM = getTagValue("GOODS_NM", e);
                item.MANF = getTagValue("MANF", e);
                item.MDL = getTagValue("MDL", e);
                item.NRGT = getTagValue("NRGT", e);
                item.GRBX = getTagValue("GRBX", e);
                item.ENDPC = getTagValue("ENDPC", e);
                item.VHCL_MLGE = getTagValue("VHCL_MLGE", e);
                item.FUEL = getTagValue("FUEL", e);
                item.SCRT_NM = getTagValue("SCRT_NM", e);
                item.TPBZ = getTagValue("TPBZ", e);
                item.ITM_NM = getTagValue("ITM_NM", e);
                item.MMB_RGT_NM = getTagValue("MMB_RGT_NM", e);
                
                List<String> imageUrls = new ArrayList<>();
                
                NodeList cltrImgFilesNodeList = e.getElementsByTagName("CLTR_IMG_FILES");
                
                if (cltrImgFilesNodeList.getLength() > 0) {
                    Element cltrImgFilesElement = (Element) cltrImgFilesNodeList.item(0);
                    NodeList cltrImgFileNodes = cltrImgFilesElement.getElementsByTagName("CLTR_IMG_FILE");

                    for (int k = 0; k < cltrImgFileNodes.getLength(); k++) {
                        Node imgFileNode = cltrImgFileNodes.item(k);
                        String imgUrl = imgFileNode.getTextContent().trim();
                        // 유효한 URL인 경우에만 리스트에 추가합니다.
                        if (!imgUrl.isEmpty() && (imgUrl.startsWith("https://") || imgUrl.startsWith("http://"))) {
                            imageUrls.add(imgUrl);
                        }
                    }
                }
                
                // 이미지 링크가 하나라도 있으면 콤마로 연결된 문자열로, 없으면 null로 저장
                if (imageUrls.isEmpty()) {
                    item.CLTR_IMG_FILES = null;
                } else {
                    item.CLTR_IMG_FILES = imageUrls.stream().collect(Collectors.joining(","));
                }

                list.add(item);
            }
        }
        return list;
    }
    
    private String getTagValue(String tag, Element e) {
        NodeList nodeList = e.getElementsByTagName(tag);
        if (nodeList.getLength() > 0 && nodeList.item(0).getFirstChild() != null) {
            return nodeList.item(0).getTextContent().trim(); 
        } else {
            return null;
        }
    }

    private void insertOnbidList(List<OnbidItem> list) throws Exception {
    	String sql = "INSERT INTO KAMCO_AUCTION_ITEMS (" +
                "RNUM, PLNM_NO, PBCT_NO, PBCT_CDTN_NO, CLTR_NO, CLTR_HSTR_NO, SCRN_GRP_CD, CTGR_FULL_NM, BID_MNMT_NO, CLTR_NM, CLTR_MNMT_NO, LDNM_ADRS, NMRD_ADRS, LDNM_PNU, DPSL_MTD_CD, DPSL_MTD_NM, BID_MTD_NM, MIN_BID_PRC, APSL_ASES_AVG_AMT, FEE_RATE, PBCT_BEGN_DTM, PBCT_CLS_DTM, PBCT_CLTR_STAT_NM, USCBD_CNT, IQRY_CNT, GOODS_NM, MANF, MDL, NRGT, GRBX, ENDPC, VHCL_MLGE, FUEL, SCRT_NM, TPBZ, ITM_NM, MMB_RGT_NM, CLTR_IMG_FILES) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false); 
            try (PreparedStatement ps = conn.prepareStatement(sql)) {

                int count = 0;
                for (OnbidItem item : list) {
                    ps.setString(1, item.RNUM);
                    ps.setString(2, item.PLNM_NO);
                    ps.setString(3, item.PBCT_NO);
                    ps.setString(4, item.PBCT_CDTN_NO);
                    ps.setString(5, item.CLTR_NO);
                    ps.setString(6, item.CLTR_HSTR_NO);
                    ps.setString(7, item.SCRN_GRP_CD);
                    ps.setString(8, item.CTGR_FULL_NM);
                    ps.setString(9, item.BID_MNMT_NO);
                    ps.setString(10, item.CLTR_NM);
                    ps.setString(11, item.CLTR_MNMT_NO);
                    ps.setString(12, item.LDNM_ADRS);
                    ps.setString(13, item.NMRD_ADRS);
                    ps.setString(14, item.LDNM_PNU);
                    ps.setString(15, item.DPSL_MTD_CD);
                    ps.setString(16, item.DPSL_MTD_NM);
                    ps.setString(17, item.BID_MTD_NM);
                    ps.setString(18, item.MIN_BID_PRC);
                    ps.setString(19, item.APSL_ASES_AVG_AMT);
                    ps.setString(20, item.FEE_RATE);
                    ps.setString(21, item.PBCT_BEGN_DTM);
                    ps.setString(22, item.PBCT_CLS_DTM);
                    ps.setString(23, item.PBCT_CLTR_STAT_NM);
                    ps.setString(24, item.USCBD_CNT);
                    ps.setString(25, item.IQRY_CNT);
                    ps.setString(26, item.GOODS_NM);
                    ps.setString(27, item.MANF);
                    ps.setString(28, item.MDL);
                    ps.setString(29, item.NRGT);
                    ps.setString(30, item.GRBX);
                    ps.setString(31, item.ENDPC);
                    ps.setString(32, item.VHCL_MLGE);
                    ps.setString(33, item.FUEL);
                    ps.setString(34, item.SCRT_NM);
                    ps.setString(35, item.TPBZ);
                    ps.setString(36, item.ITM_NM);
                    ps.setString(37, item.MMB_RGT_NM);
                    ps.setString(38, item.CLTR_IMG_FILES);
                    ps.addBatch();

                    if (++count % 1000 == 0) { 
                        ps.executeBatch();
                        conn.commit(); 
                        ps.clearBatch();
                        System.out.println("💾 " + count + "건 저장 완료 (배치)");
                    }
                }
                ps.executeBatch(); 
                conn.commit(); 
                System.out.println("💾 " + count + "건 저장 완료 (최종 배치)");
            } catch (SQLException e) {
                conn.rollback(); 
                throw e; 
            } finally {
                conn.setAutoCommit(true); 
            }
        }
    }

    // ✅ 데이터 매핑용 DTO (Inner Class)
    static class OnbidItem {
        public String RNUM;
        public String PLNM_NO;
        public String PBCT_NO;
        public String PBCT_CDTN_NO;
        public String CLTR_NO;
        public String CLTR_HSTR_NO;
        public String SCRN_GRP_CD;
        public String CTGR_FULL_NM;
        public String BID_MNMT_NO;
        public String CLTR_NM;
        public String CLTR_MNMT_NO;
        public String LDNM_ADRS;
        public String NMRD_ADRS;
        public String LDNM_PNU;
        public String DPSL_MTD_CD;
        public String DPSL_MTD_NM;
        public String BID_MTD_NM;
        public String MIN_BID_PRC;
        public String APSL_ASES_AVG_AMT;
        public String FEE_RATE;
        public String PBCT_BEGN_DTM;
        public String PBCT_CLS_DTM;
        public String PBCT_CLTR_STAT_NM;
        public String USCBD_CNT;
        public String IQRY_CNT;
        public String GOODS_NM;
        public String MANF;
        public String MDL;
        public String NRGT;
        public String GRBX;
        public String ENDPC;
        public String VHCL_MLGE;
        public String FUEL;
        public String SCRT_NM;
        public String TPBZ;
        public String ITM_NM;
        public String MMB_RGT_NM;
        public String CLTR_IMG_FILES;
    }
}