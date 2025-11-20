package com.project.app.kamco.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OnbidApiService {

	@Value("${onbid.api.base-url}")
	private String onbidApiBaseUrl;

	@Value("${onbid.api.list-endpoint}")
	private String onbidApiListEndpoint;

	@Value("${onbid.api.service-key}")
	private String onbidApiServiceKey;

	private final RestTemplate restTemplate;
	private final XmlMapper xmlMapper; // XML을 JSON으로 변환할 매퍼

	public OnbidApiService(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
		this.xmlMapper = new XmlMapper();
	}

	public Map<String, Object> getOnbidAuctionList(int numOfRows, int pageNo, String prptDvsnCd, String sido,
			String sgk, String emd, String cltrMnmtNo, String cltrNm) throws JsonProcessingException {

		URI baseUri;
		try {
			baseUri = new URI(onbidApiBaseUrl + onbidApiListEndpoint);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(
					"Invalid base URI for Onbid API: " + onbidApiBaseUrl + onbidApiListEndpoint, e);
		}
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(baseUri)
				.queryParam("serviceKey", onbidApiServiceKey).queryParam("numOfRows", numOfRows)
				.queryParam("pageNo", pageNo).queryParam("DPSL_MTD_CD", prptDvsnCd); // 온비드는 대문자 파라미터명

		if (sido != null && !sido.isEmpty()) {
			uriBuilder.queryParam("SIDO", sido);
		}
		if (sgk != null && !sgk.isEmpty()) {
			uriBuilder.queryParam("SGK", sgk);
		}
		if (emd != null && !emd.isEmpty()) {
			uriBuilder.queryParam("EMD", emd);
		}
		if (cltrMnmtNo != null && !cltrMnmtNo.isEmpty()) {
			uriBuilder.queryParam("CLTR_MNMT_NO", cltrMnmtNo);
		}
		if (cltrNm != null && !cltrNm.isEmpty()) {
			uriBuilder.queryParam("CLTR_NM", cltrNm);
		}

		// String finalUrl = uriBuilder.encode().toUriString();
		String finalUrl = uriBuilder.build().toUriString(); // <<-- encode() 호출 제거
		System.out.println("Calling Onbid API: " + finalUrl); // 백엔드 로그 확인용

		String xmlResponse = restTemplate.getForObject(finalUrl, String.class);
		// System.out.println(xmlResponse);

		// XML을 JsonNode로 변환
		JsonNode jsonNode = xmlMapper.readTree(xmlResponse);
		//System.out.println("jsonNode : " + jsonNode);

		// 원하는 데이터 구조로 파싱 (OpenAPI XML 응답 구조에 따라 정확히 조정 필요)
		// 예시: <response><body ...></response>
		JsonNode bodyNode = jsonNode.path("body");
		//System.out.println("bodyNode : " + bodyNode);

		// totalCount 추출
		int totalCount = bodyNode.path("totalCount").asInt(0);

		// item 리스트 추출
		// <items><item>...</item><item>...</item></items>
		JsonNode itemsNode = bodyNode.path("items").path("item");
		// 아이템들을 List<Map<String, Object>> 형태로 변환
		// 여기서 XML 키(SNAKE_CASE)를 JSON 키(camelCase)로 변환하는 로직을 구현할 수 있습니다.
		List<Map<String, Object>> itemList = new ArrayList<>();
		if (itemsNode.isArray()) {
			for (JsonNode item : itemsNode) {
				itemList.add(convertNodeToCamelCaseMap(item)); // 사용자 정의 유틸리티 함수
			}
		} else if (itemsNode.isObject()) { // 아이템이 하나인 경우 배열이 아닌 객체로 올 수 있음
			itemList.add(convertNodeToCamelCaseMap(itemsNode));
		}

		Map<String, Object> result = new HashMap<>();
		result.put("totalCount", totalCount);
		result.put("items", itemList);
		return result;
	}

	// JsonNode의 키를 CamelCase로 변환하는 유틸리티 메서드 (재귀)
	private Map<String, Object> convertNodeToCamelCaseMap(JsonNode node) {
		Map<String, Object> map = new HashMap<>();
        
        Iterator<String> fieldNames = node.fieldNames();

        while (fieldNames.hasNext()) {
            String originalKey = fieldNames.next();
            JsonNode valueNode = node.get(originalKey);
            String camelCaseKey = toCamelCase(originalKey); 

            // =========================================================
            //  클래스 내부의 imageLinks 처리 로직 시작
            // =========================================================
            if (camelCaseKey.equals("cltrImgFiles")) { // <- 부모 필드명은 여전히 camelCase인 'cltrImgFiles'
                List<Map<String, String>> imageLinks = new ArrayList<>();
                String originalCltrImgFilesValue = ""; 

                // valueNode가 객체이고, 그 객체 안에 "CLTR_IMG_FILE" 필드가 있는 경우에만 이미지 링크를 파싱
                // 여기서 필드명 "CLTR_IMG_FILE"는 JSON에 파싱된 그대로의 대문자를 사용합니다.
                if (valueNode.isObject() && valueNode.has("CLTR_IMG_FILE")) {

                    JsonNode imgFileNode = valueNode.path("CLTR_IMG_FILE");
                    
                    if (imgFileNode.isArray()) { 
                        int seqCounter = 1;
                        for (JsonNode imgUrlNode : imgFileNode) {
                            String imgUrl = imgUrlNode.asText("").trim();
                            if (!imgUrl.isEmpty() && (imgUrl.startsWith("https://") || imgUrl.startsWith("http://"))) {
                                Map<String, String> linkInfo = new HashMap<>();
                                linkInfo.put("seq", String.valueOf(seqCounter++));
                                linkInfo.put("url", imgUrl);
                                imageLinks.add(linkInfo);
                            }
                        }
                    } else if (imgFileNode.isValueNode()) { 
                        String imgUrl = imgFileNode.asText("").trim();
                        if (!imgUrl.isEmpty() && (imgUrl.startsWith("https://") || imgUrl.startsWith("http://"))) {
                            Map<String, String> linkInfo = new HashMap<>();
                            linkInfo.put("seq", "1");
                            linkInfo.put("url", imgUrl);
                            imageLinks.add(linkInfo);
                        }
                    }
                    originalCltrImgFilesValue = valueNode.toPrettyString(); 
                } else {
                    originalCltrImgFilesValue = "";
                }
                
                map.put("imageLinks", imageLinks); 
                map.put(camelCaseKey, originalCltrImgFilesValue); 
            }
            else if (valueNode.isObject()) {
                map.put(camelCaseKey, convertNodeToCamelCaseMap(valueNode));
            } else if (valueNode.isArray()) {
                List<Object> list = new ArrayList<>();
                for (JsonNode element : valueNode) {
                    if (element.isObject()) {
                        list.add(convertNodeToCamelCaseMap(element));
                    } else if (element.isValueNode()) {
                        list.add(element.asText("").trim());
                    } else {
                        list.add(element.toString().trim()); 
                    }
                }
                map.put(camelCaseKey, list);
            } else if (valueNode.isValueNode()) {
                map.put(camelCaseKey, valueNode.asText("").trim());
            } else {
                 map.put(camelCaseKey, ""); 
            }
        }
        return map;
    }

	// SNAKE_CASE to camelCase 변환 메서드 (OnbidApiService 클래스 내부에)
	private String toCamelCase(String snakeCase) {
		if (snakeCase == null || snakeCase.isEmpty()) {
			return "";
		}
		StringBuilder camelCase = new StringBuilder();
		boolean nextCharUpperCase = false;
		for (int i = 0; i < snakeCase.length(); i++) {
			char c = snakeCase.charAt(i);
			if (c == '_') {
				nextCharUpperCase = true;
			} else {
				if (nextCharUpperCase) {
					camelCase.append(Character.toUpperCase(c));
					nextCharUpperCase = false;
				} else {
					camelCase.append(Character.toLowerCase(c));
				}
			}
		}
		return camelCase.toString();
	}
}