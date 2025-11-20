// src/components/pages/DashboardPage.jsx
import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useSelector } from 'react-redux';
import { Navigate } from 'react-router-dom';
import DetailModal from '../common/DetailModal.jsx';
import api from '../../api/index.js';
import { formatCurrency, formatDateTime } from '../../util/formatters';
import '../../css/BoardPage.css';

// 필드 정의: 각 컬럼에 대한 정보 (헤더 이름, 데이터 키, 정렬 가능 여부 등)
const FIELD_DEFINITIONS = [
    { key: "cltrMnmtNo", title: "물건관리번호" },
    { key: "cltrHstrNo", title: "물건이력번호", sortable: true },
    { key: "ctgrFullNm", title: "카테고리" },
    { key: "cltrNm", title: "물건명" },
    { key: "feeRate", title: "수수료율" },
    { key: "pbctBegnDtm", title: "공고시작일시" },
    { key: "pbctClsDtm", title: "공고종료일시" },
    { key: "bidAmount", title: "나의입찰가" },
];


function BidBoardPage() {
    const { isAuthenticated } = useSelector(state => state.auth);
    const [auctionItems, setAuctionItems] = useState([]); // API로부터 받은 원본 데이터
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const userId = localStorage.getItem('userId');

    // 필터링 및 페이징 상태
    const [currentPage, setCurrentPage] = useState(1);
    const [totalCount, setTotalCount] = useState(0);
    const [selectGbn, setSelectGbn] = useState(''); // 조회구분
    const [cltrNm, setCltrNm] = useState(''); // 물건명 검색어

    // 페이지네이션 관련 상수
    const DEFAULT_NUM_OF_ROWS = 10;
    const PAGE_RANGE_SIZE = 10;

    // 모달 관련 상태
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [selectedItem, setSelectedItem] = useState(null);

    // ======== 정렬 관련 상태 ========
    const [sortKey, setSortKey] = useState(null); // 현재 정렬 기준 컬럼 키
    const [sortDirection, setSortDirection] = useState('asc'); // 'asc' (오름차순) | 'desc' (내림차순)



    // API 호출 함수 ( useCallback으로 래핑하여 불필요한 재생성 방지)
    const fetchAuctionItems = useCallback(async (page, currentFilters) => {
        setLoading(true);
        setError(null);
        try {
            const params = {
                numOfRows: DEFAULT_NUM_OF_ROWS,
                pageNo: page,
                userId: userId,
            };

            if (currentFilters.cltrNm) params.cltrNm = currentFilters.cltrNm;
            if (currentFilters.selectGbn) params.selectGbn = currentFilters.selectGbn;

            // console.log("백엔드 프록시 API 호출 파라미터:", params);
            const response = await api.get('/kamco/getMyList', { params });

            // console.log("kamco 데이터 응답:", response.data);
            setAuctionItems(response.data || []); // 받아온 데이터를 저장
            setTotalCount(response.data.length || 0);

        } catch (err) {
            // console.error("kamco 데이터 로드 중 오류 발생:", err);
            setError("kamco 데이터를 불러오는데 실패했습니다. 백엔드 프록시 API를 확인해주세요.");
            setAuctionItems([]);
            setTotalCount(0);
        } finally {
            setLoading(false);
        }
    }, [DEFAULT_NUM_OF_ROWS]); // DEFAULT_NUM_OF_ROWS는 상수이므로 여기에 넣어줍니다.

    // 컴포넌트 첫 로드 및 인증 상태 변경 시 데이터 로드
    useEffect(() => {
        if (!isAuthenticated) { // 인증되지 않았다면 API 호출 안함
            return;
        }
        // 첫 로드 시 현재 필터 상태를 묶어서 fetchAuctionItems에 전달하여 조회
        fetchAuctionItems(currentPage, { userId, cltrNm });
    }, [isAuthenticated, fetchAuctionItems, currentPage, userId, cltrNm]);


    // 검색 실행 및 페이지 변경을 담당하는 함수 (useCallback으로 래핑)
    const triggerSearch = useCallback((pageToFetch = 1) => {
        setCurrentPage(pageToFetch); // 현재 페이지 상태 업데이트

        const currentFilters = { // 현재 필터 값들을 객체로 묶음
            userId, cltrNm, selectGbn,
        };
        fetchAuctionItems(pageToFetch, currentFilters); // API 호출
    }, [userId, cltrNm, selectGbn, fetchAuctionItems]);


    // 검색 버튼 클릭 핸들러
    const handleSearchButtonClick = () => {
        triggerSearch(1); // 검색 버튼 클릭 시 1페이지부터 다시 검색 시작
    };

    // 모달 열기 함수 (useCallback으로 래핑)
    const openDetailModal = useCallback((item) => {
        setSelectedItem(item);
        setIsModalOpen(true);
    }, []);

    // 모달 닫기 함수 (useCallback으로 래핑)
    const closeDetailModal = useCallback(() => {
        setIsModalOpen(false);
        setSelectedItem(null); // 선택된 아이템 초기화
    }, []);


    // ======== 정렬 핸들러 ========
    const handleSort = useCallback((key) => {
        if (!key) return; // 유효한 키가 아니면 정렬하지 않음

        // 현재 정렬 기준 키와 동일하면 정렬 방향만 변경 (오름차순 <-> 내림차순)
        if (sortKey === key) {
            setSortDirection(prevDirection => (prevDirection === 'asc' ? 'desc' : 'asc'));
        } else { // 새로운 키로 정렬 시, 기본은 오름차순으로 시작
            setSortKey(key);
            setSortDirection('asc');
        }
    }, [sortKey]); // sortKey가 변경될 때만 재생성


    // ======== 정렬된 아이템 목록 계산 ========
    // auctionItems, sortKey, sortDirection 중 하나라도 변경될 때만 다시 계산
    const sortedAuctionItems = useMemo(() => {
        // 정렬 기준이 없거나 데이터가 비어있으면 원본 데이터를 그대로 반환
        if (!sortKey || auctionItems.length === 0) {
            return auctionItems;
        }

        const sorted = [...auctionItems].sort((a, b) => { // 원본 배열을 복사하여 정렬
            const valA = a[sortKey];
            const valB = b[sortKey];

            // null 또는 undefined 값 처리 (정렬 시 가장 마지막으로 보내는 것이 일반적)
            if (valA === null || valA === undefined) return sortDirection === 'asc' ? 1 : -1;
            if (valB === null || valB === undefined) return sortDirection === 'asc' ? -1 : 1;

            // 숫자형 필드 정렬 로직 (예: minBidPrc, iqryCnt)
            if (typeof valA === 'number' && typeof valB === 'number') {
                return sortDirection === 'asc' ? valA - valB : valB - valA;
            }
            // 날짜/시간 문자열 필드 정렬 로직 (예: pbctBegnDtm, pbctClsDtm)
            if (sortKey.includes('Dtm')) { // 'Dtm'이 포함된 필드는 날짜/시간 문자열로 간주
                const dateA = new Date(valA);
                const dateB = new Date(valB);
                if (isNaN(dateA.getTime()) || isNaN(dateB.getTime())) { // 날짜 변환이 유효하지 않은 경우 비교하지 않음
                    return 0;
                }
                // 날짜 객체의 타임스탬프를 사용하여 비교
                return sortDirection === 'asc' ? dateA.getTime() - dateB.getTime() : dateB.getTime() - dateA.getTime();
            }
            // 그 외 문자열 필드 정렬 로직
            // toLowerCase()로 대소문자 구분 없이, localeCompare()로 한국어 정렬 지원
            const strA = String(valA).toLowerCase();
            const strB = String(valB).toLowerCase();
            return sortDirection === 'asc' ? strA.localeCompare(strB) : strB.localeCompare(strA);
        });
        return sorted;
    }, [auctionItems, sortKey, sortDirection]); // 의존성 배열에 관련 상태들을 추가


    // 페이지네이션 컴포넌트 (ApiBoardPage 내부에 정의)
    const Pagination = () => {
        const totalPages = Math.ceil(totalCount / DEFAULT_NUM_OF_ROWS); // 총 페이지 수 계산
        if (totalPages <= 1) return null; // 페이지가 1개 이하면 페이지네이션 미표시

        const currentBlock = Math.ceil(currentPage / PAGE_RANGE_SIZE); // 현재 페이지 블록 계산
        const startPageInBlock = (currentBlock - 1) * PAGE_RANGE_SIZE + 1; // 현재 블록의 시작 페이지
        const endPageInBlock = Math.min(totalPages, currentBlock * PAGE_RANGE_SIZE); // 현재 블록의 마지막 페이지

        const pageNumbers = []; // 현재 블록에 표시할 페이지 번호 배열
        for (let i = startPageInBlock; i <= endPageInBlock; i++) {
            pageNumbers.push(i);
        }

        return (
            <div className="board-pagination">
                <button onClick={() => triggerSearch(1)} disabled={currentPage === 1} className="board-pagination-button">&lt;&lt;</button>
                <button onClick={() => triggerSearch(Math.max(1, startPageInBlock - PAGE_RANGE_SIZE))} disabled={startPageInBlock === 1} className="board-pagination-button">&lt;</button>
                {pageNumbers.map(number => (
                    <button key={number} onClick={() => triggerSearch(number)} className={`board-pagination-button ${currentPage === number ? 'active' : ''}`}>{number}</button>
                ))}
                <button onClick={() => triggerSearch(Math.min(totalPages, endPageInBlock + PAGE_RANGE_SIZE))} disabled={endPageInBlock === totalPages} className="board-pagination-button">&gt;</button>
                <button onClick={() => triggerSearch(totalPages)} disabled={currentPage === totalPages} className="board-pagination-button">&gt;&gt;</button>
            </div>
        );
    };




    return (
        <div className="board-container">
            <h1 className="board-header">나의 물건 조회</h1>

            <div className="board-filter-container">
                <label className="board-label">
                    물건 목록
                    <select value={selectGbn} onChange={(e) => { setSelectGbn(e.target.value); setCurrentPage(1); }} className="board-select">
                        <option value="">전체</option>
                        <option value="favorite">관심</option>
                        <option value="bid">입찰</option>
                    </select>
                </label>
                <div className="board-filter-group-wide">
                    <label htmlFor="cltrNm" className="board-label">물건명 부분 검색</label>
                    <input type="text" id="cltrNm" value={cltrNm} onChange={(e) => setCltrNm(e.target.value)} placeholder="물건 부분 검색" className="board-input" />
                </div>
                (총 {totalCount}건)
                <div className="board-search-button-group">
                    <button onClick={handleSearchButtonClick} className="board-search-button">조회</button>
                </div>
            </div>

            {loading && <p className="board-loading">데이터를 불러오는 중입니다...</p>}
            {error && <p className="board-error">에러: {error}</p>}
            {!loading && !error && auctionItems.length === 0 && <p className="board-no-data">조회된 데이터가 없습니다.</p>}

            {!loading && !error && auctionItems.length > 0 && (
                <div className="board-table-container">
                    <table className="board-table">
                        <thead>
                            <tr>
                                {FIELD_DEFINITIONS.map((field, index) => (
                                    <th key={index}>
                                        {field.sortable ? (
                                            <span onClick={() => handleSort(field.key)} className="board-sortable">
                                                {field.title}{sortKey === field.key && <span>{sortDirection === 'asc' ? ' ▲' : ' ▼'}</span>}
                                            </span>
                                        ) : field.title}
                                    </th>
                                ))}
                            </tr>
                        </thead>
                        <tbody>
                            {sortedAuctionItems.map((item, itemIndex) => (
                                <tr key={itemIndex}>
                                    {FIELD_DEFINITIONS.map((field, colIndex) => (
                                        <td key={colIndex}>
                                            {field.key === "cltrMnmtNo" ? (
                                                <span onClick={() => openDetailModal(item)} className="board-link">{item[field.key] || '-'}</span>
                                            ) : field.key === "bidAmount" ? formatCurrency(item[field.key])
                                                : field.key === "pbctBegnDtm" || field.key === "pbctClsDtm" ? formatDateTime(item[field.key])
                                                    : field.key === "imageLinks" ? (
                                                        item.imageLinks && item.imageLinks.length > 0 ? (
                                                            <div className="board-image-links">
                                                                {item.imageLinks.map((linkInfo, idx) => (
                                                                    <a key={idx} href={linkInfo.url} target="_blank" rel="noopener noreferrer" className="board-image-link">
                                                                        {String(idx + 1).padStart(2, '0')}
                                                                    </a>
                                                                ))}
                                                            </div>
                                                        ) : '-'
                                                    ) : item[field.key] !== undefined && item[field.key] !== null ? String(item[field.key]) : ''}
                                        </td>
                                    ))}
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            <Pagination />
            {isModalOpen && <DetailModal isOpen={isModalOpen} onClose={closeDetailModal} item={selectedItem} openPage="bidBoard" onSearch={handleSearchButtonClick} />}
        </div>
    );
}

export default BidBoardPage;