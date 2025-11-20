// src/components/common/DetailModal.jsx
import React, { useState, useEffect, useCallback, useRef } from 'react';
import api from '../../api';
import { formatCurrency, formatBidAmountInput, getImageUrls } from '../../util/formatters';
import '../../css/DetailModal.css';

const MESSAGES = {
    LOGIN_REQUIRED_BID: '로그인 후 입찰할 수 있습니다.',
    LOGIN_REQUIRED_FAVORITE: '로그인 후 관심목록을 할 수 있습니다.',
    INVALID_BID_AMOUNT: '유효한 입찰 금액을 입력해주세요.',
    BID_SUCCESS: '입찰이 성공적으로 처리되었습니다.',
    BID_ERROR: '입찰 처리 중 오류가 발생했습니다.',
    FAVORITE_ERROR: '관심목록 처리 중 오류가 발생했습니다.',
};

const DetailModal = ({ isOpen, onClose, item, openPage, onSearch }) => {
    const [currentItem, setCurrentItem] = useState(item);

    const [bidAmount, setBidAmount] = useState('');
    const [userSavedBidAmount, setUserSavedBidAmount] = useState(null);
    const [isFavorite, setIsFavorite] = useState(false);
    const [isBid, setIsBid] = useState(false);
    const [auctionItems, setAuctionItems] = useState([]);

    const [isLoadingMyData, setIsLoadingMyData] = useState(true);
    const [isLoadingAuctionData, setIsLoadingAuctionData] = useState(false);

    const userId = localStorage.getItem('userId');

    const createModifyParams = (additionalParams = {}) => ({
        userId,
        cltrMnmtNo: currentItem.cltrMnmtNo,
        cltrHstrNo: currentItem.cltrHstrNo,
        ctgrFullNm: currentItem.ctgrFullNm,
        cltrNm: currentItem.cltrNm,
        pbctBegnDtm: currentItem.pbctBegnDtm,
        pbctClsDtm: currentItem.pbctClsDtm,
        feeRate: currentItem.feeRate,
        ...additionalParams,
    });

    // 📢 API 호출 제어를 위한 useRef 플래그
    // 각 타입의 API가 현재 item에 대해 호출되었는지 추적합니다.
    const hasFetchedMyDataRef = useRef(false);
    const hasFetchedAuctionDataRef = useRef(false);

    // currentItem이 변경될 때마다 imageUrls 갱신
    const imageUrls = getImageUrls(currentItem?.cltrImgFiles);

    // 📢 prop 'item'이 변경될 때 currentItem을 업데이트하고 관련 상태를 초기화합니다.
    // 이 useEffect는 오직 prop 'item'이 변경될 때만 실행됩니다.
    // 여기서 API 호출 플래그도 초기화하여 새 item이 오면 다시 API를 호출할 수 있도록 합니다.
    useEffect(() => {
        // item prop이 유효하고, 현재 currentItem과 식별자가 다를 때 (새로운 아이템을 받음)
        // 또는 currentItem이 아직 설정되지 않았을 때 (모달 최초 열림 시)
        if (item && (item.cltrMnmtNo !== currentItem?.cltrMnmtNo || item.cltrHstrNo !== currentItem?.cltrHstrNo || !currentItem)) {
            setCurrentItem(item);

            // 📢 새 아이템이 들어왔으니 모든 플래그와 UI 상태를 초기화
            hasFetchedMyDataRef.current = false;
            hasFetchedAuctionDataRef.current = false;

            setBidAmount('');
            setUserSavedBidAmount(null);
            setIsFavorite(false);
            setIsBid(false);
            setIsLoadingMyData(true);
            setIsLoadingAuctionData(false);
            setAuctionItems([]);
        }
    }, [item]);


    // ===============================================
    // ==== (1) 백엔드 API (getMyDataStatus) 호출: 사용자별 즐겨찾기/입찰 정보 조회 ====
    const fetchUserItemData = useCallback(async (itemToFetch) => { // 📢 itemToFetch를 인자로 받음
        if (!userId || !itemToFetch?.cltrMnmtNo || !itemToFetch?.cltrHstrNo || hasFetchedMyDataRef.current) { // 📢 플래그 확인
            setIsLoadingMyData(false);
            return;
        }

        setIsLoadingMyData(true);
        try {
            const response = await api.get('/kamco/getMyDataStatus', {
                params: {
                    userId,
                    cltrMnmtNo: itemToFetch.cltrMnmtNo,
                    cltrHstrNo: itemToFetch.cltrHstrNo,
                }
            });

            const data = response.data;
            setIsFavorite(data.isFavorite === 'Y');
            setIsBid(data.isBid === 'Y');

            if (data.bidAmount) {
                setUserSavedBidAmount(data.bidAmount);
                setBidAmount(formatBidAmountInput(String(data.bidAmount)));
            } else {
                setUserSavedBidAmount(null);
                setBidAmount('');
            }
            hasFetchedMyDataRef.current = true; // 📢 API 호출 성공 플래그 설정
        } catch (error) {
            console.error("사용자별 물건 데이터 로드 중 오류 발생:", error);
            setIsFavorite(false);
            setIsBid(false);
            setUserSavedBidAmount(null);
            setBidAmount('');
        } finally {
            setIsLoadingMyData(false);
        }
    }, [userId]); // 📢 의존성: userId (currentItem 제거)


    // ===============================================
    // ==== (2) 백엔드 프록시 API (onbid/list) 호출: 최신 물건 상세 정보 및 히스토리 조회 ====
    const fetchAuctionItem = useCallback(async (itemToFetch) => { // 📢 itemToFetch를 인자로 받음
        if (!itemToFetch?.cltrMnmtNo || !itemToFetch?.cltrHstrNo || hasFetchedAuctionDataRef.current) { // 📢 플래그 확인
            setIsLoadingAuctionData(false);
            return;
        }

        setIsLoadingAuctionData(true);
        try {
            const params = {
                numOfRows: 10,
                pageNo: 1,
                cltrMnmtNo: itemToFetch.cltrMnmtNo,
                cltrNm: itemToFetch.cltrNm,
            };

            // console.log("백엔드 프록시 API 호출 파라미터:", params);
            const response = await api.get('/onbid/list', { params });
            // console.log("온비드 데이터 응답:", response.data);

            //setAuctionItems(response.data.items || []);

            // 📢 cltrHstrNo를 기준으로 내림차순 정렬하는 로직 추가
            const fetchedItems = response.data.items || [];
            if (fetchedItems.length > 0) {
                fetchedItems.sort((a, b) => {
                    // cltrHstrNo는 문자열일 수 있으므로 숫자로 변환하여 비교합니다.
                    // 유효하지 않은 경우를 대비하여 0으로 처리하거나 다른 방식으로 비교 기준을 세울 수 있습니다.
                    const hstrNoA = parseInt(a.cltrHstrNo, 10);
                    const hstrNoB = parseInt(b.cltrHstrNo, 10);

                    if (isNaN(hstrNoA) || isNaN(hstrNoB)) {
                        // 숫자로 변환할 수 없는 경우, 문자열로 비교하거나 다른 기준 적용
                        return String(b.cltrHstrNo).localeCompare(String(a.cltrHstrNo));
                    }
                    return hstrNoB - hstrNoA; // 내림차순 정렬
                });
            }
            setAuctionItems(fetchedItems); // 정렬된 배열을 상태에 저장

            const foundItem = (response.data.items || []).find(
                (auction) =>
                    auction.cltrMnmtNo === itemToFetch.cltrMnmtNo &&
                    auction.cltrHstrNo === itemToFetch.cltrHstrNo
            );

            if (foundItem) {
                setCurrentItem(foundItem); // 📢 내부 상태 currentItem을 최신 데이터로 업데이트
                // console.log("상세 모달에 최신 업데이트된 아이템:", foundItem);
            } else {
                // console.warn("API에서 해당 물건의 최신 정보를 찾을 수 없습니다:", itemToFetch);
            }
            hasFetchedAuctionDataRef.current = true; // 📢 API 호출 성공 플래그 설정
        } catch (err) {
            console.error("온비드 데이터 로드 중 오류 발생:", err);
        } finally {
            setIsLoadingAuctionData(false);
        }
    }, [setCurrentItem]); // 📢 의존성: setCurrentItem (setItemToFetch 제거)


    // 📢 모든 API 호출을 제어하는 주 useEffect
    // 이 useEffect는 모달의 열림 상태와 현재 아이템이 유효한지 여부를 주로 감시합니다.
    useEffect(() => {
        // 모달이 닫혀있거나, item prop이 유효하지 않으면 아무것도 하지 않음
        if (!isOpen || !currentItem?.cltrMnmtNo || !currentItem?.cltrHstrNo) {
            // 📢 모달이 닫히면 모든 플래그와 UI 상태를 초기화
            if (!isOpen) {
                hasFetchedMyDataRef.current = false;
                hasFetchedAuctionDataRef.current = false;
                setCurrentItem(null); // 모달이 닫히면 currentItem도 null로 초기화하여 다음 오픈 시 새롭게 시작
                setBidAmount('');
                setUserSavedBidAmount(null);
                setIsFavorite(false);
                setIsBid(false);
                setIsLoadingMyData(true);
                setIsLoadingAuctionData(false);
                setAuctionItems([]);
            }
            return;
        }

        // 📢 현재 currentItem에 대해 사용자 데이터를 아직 가져오지 않았다면 호출
        if (!hasFetchedMyDataRef.current) {
            fetchUserItemData(currentItem);
        }

        // 📢 'bidBoard' 페이지이고, currentItem에 대해 auction 데이터를 아직 가져오지 않았다면 호출
        if (openPage === 'bidBoard' && !hasFetchedAuctionDataRef.current) {
            fetchAuctionItem(currentItem);
        } else if (openPage !== 'bidBoard') {
            // 'bidBoard'가 아닐 때는 온비드 데이터 로딩이 필요 없으므로 로딩 상태와 히스토리 초기화
            setIsLoadingAuctionData(false);
            setAuctionItems([]);
        }
    }, [isOpen, currentItem, openPage, fetchUserItemData, fetchAuctionItem]); // 📢 의존성: isOpen, currentItem, openPage, fetchUserItemData, fetchAuctionItem


    const handleBidClick = async () => {
        if (!userId) {
            alert(MESSAGES.LOGIN_REQUIRED_BID);
            return;
        }

        const parsedBidAmount = parseInt(bidAmount.replace(/,/g, ''), 10);
        if (isNaN(parsedBidAmount) || parsedBidAmount <= 0) {
            alert(MESSAGES.INVALID_BID_AMOUNT);
            return;
        }

        const minBidPrc = parseInt(String(currentItem.minBidPrc || '0').replace(/[^0-9]/g, ''), 10);
        if (parsedBidAmount < minBidPrc) {
            alert(`입찰 금액은 최저입찰가(${formatCurrency(minBidPrc)})보다 높아야 합니다.`);
            return;
        }

        if (window.confirm(`${currentItem.cltrNm || '물건'}에 ${formatCurrency(parsedBidAmount)}원으로 입찰하시겠습니까?`)) {
            try {
                const params = createModifyParams({
                    isBid: 'Y',
                    bidAmount: parsedBidAmount,

                });

                const response = await api.post('/kamco/modifyMyData', params);
                if (response.status === 200) {
                    alert(MESSAGES.BID_SUCCESS);
                    setUserSavedBidAmount(parsedBidAmount);
                    setBidAmount(formatBidAmountInput(String(parsedBidAmount)));
                    setIsBid(true);
                    if (openPage === 'bidBoard') { // 관심목록 변경 후 'bidBoard' 페이지일 때만 검색 함수 호출
                        onSearch(); // 📢 입찰 성공 후 부모 컴포넌트의 검색 함수 호출
                    }
                } else {
                    alert(MESSAGES.BID_ERROR);
                }
            } catch (error) {
                console.error('입찰 처리 중 오류 발생:', error);
                alert(MESSAGES.BID_ERROR);
            }
        }
    };

    const handleBidAmountChange = (e) => {
        setBidAmount(formatBidAmountInput(e.target.value));
    };

    const handleFavoriteClick = async () => {
        if (!userId) {
            alert(MESSAGES.LOGIN_REQUIRED_FAVORITE);
            return;
        }

        const newFavoriteStatus = !isFavorite;
        const confirmMessage = newFavoriteStatus ?
            `${currentItem.cltrNm || '물건'}을(를) 관심목록에 추가하시겠습니까?` :
            `${currentItem.cltrNm || '물건'}을(를) 관심목록에서 제거하시겠습니까?`;

        if (window.confirm(confirmMessage)) {
            try {
                const params = createModifyParams({
                    isFavorite: newFavoriteStatus ? 'Y' : 'N',
                    isBid: isBid ? 'Y' : 'N',
                    bidAmount: userSavedBidAmount,
                });

                const response = await api.post('/kamco/modifyMyData', params);
                if (response.status === 200) {
                    setIsFavorite(newFavoriteStatus);
                    if (openPage === 'bidBoard') { // 관심목록 변경 후 'bidBoard' 페이지일 때만 검색 함수 호출
                        onSearch(); // 📢 성공 후 부모 컴포넌트의 검색 함수 호출
                    }

                } else {
                    alert(MESSAGES.FAVORITE_ERROR);
                }
            } catch (error) {
                console.error('관심목록 처리 중 오류 발생:', error);
                alert(MESSAGES.FAVORITE_ERROR);
            }
        }
    };


    // 모달이 열려있지 않으면 아무것도 렌더링하지 않음
    if (!isOpen) return null;

    // 모달은 열려있는데 currentItem 데이터가 아직 유효하지 않거나 로딩 중일 때 로딩 UI 표시
    if (!currentItem || !currentItem.cltrMnmtNo || isLoadingMyData || isLoadingAuctionData) {
        return (
            <div className="detail-modal-overlay">
                <div className="detail-modal">
                    <button onClick={onClose} className="detail-modal-close">&times;</button>
                    <div className="detail-loading">
                        <p>해당 물건의 상세 내역과 이력 데이터를 불러오는 중입니다...</p>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="detail-modal-overlay">
            <div className="detail-modal">
                <button onClick={onClose} className="detail-modal-close">&times;</button>
                <h2 className="detail-modal-title">🏢 {currentItem.cltrNm || '물건 정보'}</h2>

                <div className="detail-modal-actions">
                    <div className="detail-modal-actions-left">
                        <button
                            onClick={handleFavoriteClick}
                            className={`detail-btn detail-btn-favorite ${isFavorite ? 'active' : ''}`}
                        >
                            {isFavorite ? '⭐ 관심목록 해제' : '☆ 관심목록'}
                        </button>
                    </div>

                    <div className="detail-modal-actions-right">
                        <div className="detail-bid-input-group">
                            <input
                                type="text"
                                placeholder={`입찰금액 (최저: ${formatCurrency(currentItem.minBidPrc)})`}
                                value={bidAmount}
                                onChange={handleBidAmountChange}
                                className="detail-bid-input"
                                inputMode="numeric"
                                readOnly={isBid}
                            />
                            <button
                                onClick={handleBidClick}
                                className="detail-btn detail-btn-bid"
                                disabled={isBid}
                            >
                                {isBid ? '✅ 입찰완료' : '💰 입찰'}
                            </button>
                        </div>
                    </div>
                </div>

                {isBid && userSavedBidAmount !== null && (
                    <div className="detail-bid-status">
                        💵 나의 입찰액: {formatCurrency(userSavedBidAmount)}
                    </div>
                )}

                <div className="detail-info-section">
                    <h3 className="detail-info-title">📋 상세 정보</h3>
                    <table className="detail-table">
                        <tbody>
                            <tr>
                                <td className="detail-table-label">물건명</td>
                                <td className="detail-table-value">{currentItem.cltrNm || '-'}</td>
                                <td className="detail-table-label">카테고리</td>
                                <td className="detail-table-value">{currentItem.ctgrFullNm || '-'}</td>
                            </tr>
                            <tr>
                                <td className="detail-table-label">물건관리번호</td>
                                <td className="detail-table-value">{currentItem.cltrMnmtNo || '-'}</td>
                                <td className="detail-table-label">처분방법</td>
                                <td className="detail-table-value">{currentItem.dpslMtdNm || '-'}</td>
                            </tr>
                            <tr>
                                <td className="detail-table-label">공고번호</td>
                                <td className="detail-table-value">{currentItem.pbctNo || '-'}</td>
                                <td className="detail-table-label">공고조건번호</td>
                                <td className="detail-table-value">{currentItem.pbctCdtnNo || '-'}</td>
                            </tr>
                            <tr>
                                <td className="detail-table-label">공매계획번호</td>
                                <td className="detail-table-value">{currentItem.plnmNo || '-'}</td>
                                <td className="detail-table-label">물건번호</td>
                                <td className="detail-table-value">{currentItem.cltrNo || '-'}</td>
                            </tr>
                            <tr>
                                <td className="detail-table-label">입찰방법명</td>
                                <td className="detail-table-value">{currentItem.bidMtdNm || '-'}</td>
                                <td className="detail-table-label">최저입찰가</td>
                                <td className="detail-table-value">{formatCurrency(currentItem.minBidPrc)}</td>
                            </tr>
                            <tr>
                                <td className="detail-table-label">도로명주소</td>
                                <td className="detail-table-value" colSpan="3">{currentItem.nmrdAdrs || '-'}</td>
                            </tr>
                            <tr>
                                <td className="detail-table-label">지번주소</td>
                                <td className="detail-table-value" colSpan="3">{currentItem.ldnmAdrs || '-'}</td>
                            </tr>
                            <tr>
                                <td className="detail-table-label">감정평가금액</td>
                                <td className="detail-table-value">{formatCurrency(currentItem.apslAsesAvgAmt)}</td>
                                <td className="detail-table-label">수수료율</td>
                                <td className="detail-table-value">{currentItem.feeRate || '-'}</td>
                            </tr>
                            <tr>
                                <td className="detail-table-label">공고시작일시</td>
                                <td className="detail-table-value">{currentItem.pbctBegnDtm || '-'}</td>
                                <td className="detail-table-label">공고종료일시</td>
                                <td className="detail-table-value">{currentItem.pbctClsDtm || '-'}</td>
                            </tr>
                            <tr>
                                <td className="detail-table-label">공고물건상태</td>
                                <td className="detail-table-value">{currentItem.pbctCltrStatNm || '-'}</td>
                                <td className="detail-table-label">유찰회수</td>
                                <td className="detail-table-value">{currentItem.uscbdCnt || '-'}</td>
                            </tr>
                            <tr>
                                <td className="detail-table-label">조회건수</td>
                                <td className="detail-table-value">{currentItem.iqryCnt || '-'}</td>
                                <td className="detail-table-label"></td>
                                <td className="detail-table-value"></td>
                            </tr>

                            {/* 이미지 다운로드 링크 섹션 */}
                            <tr>
                                <td className="detail-table-label">이미지</td>
                                <td className="detail-table-value" colSpan="3">
                                    <div className="detail-image-grid">
                                        {/* 📢 currentItem.imageLinks가 배열이고 내용이 있을 때만 map 함수 호출 */}
                                        {currentItem.imageLinks && currentItem.imageLinks.length > 0 ? (
                                            currentItem.imageLinks.map((linkInfo, idx) => (
                                                <a
                                                    key={idx}
                                                    href={linkInfo.url} // linkInfo 객체의 url 속성을 사용
                                                    target="_blank"
                                                    rel="noopener noreferrer"
                                                    className="detail-image-link"
                                                >
                                                    이미지 {String(idx + 1).padStart(2, '0')}
                                                </a>
                                            ))
                                        ) : (
                                            <span className="no-image-message">등록된 이미지가 없습니다.</span>
                                        )}
                                    </div>
                                </td>
                            </tr>

                        </tbody>
                    </table>
                </div>

                {currentItem.goodsNm && (
                    <div className="detail-info-section">
                        <h3 className="detail-info-title">📝 물품명세 상세 설명</h3>
                        <p className="detail-description-content">{currentItem.goodsNm}</p>
                    </div>
                )}

                {auctionItems.length > 0 && openPage === 'bidBoard' && (
                    <div className="detail-info-section">
                        <h3 className="detail-info-title">📜 물건 이력 정보 ({currentItem.cltrMnmtNo})</h3>
                        <div className="detail-history-table-container">
                            <table className="detail-history-table">
                                <thead>
                                    <tr>
                                        <th>이력</th>
                                        <th>이력번호</th>
                                        <th>수수료율</th>
                                        <th>최저입찰가</th>
                                        <th>공고시작일시</th>
                                        <th>공고종료일시</th>
                                        <th>공고물건상태</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {auctionItems.map((historyItem, idx) => (
                                        <tr key={idx}>
                                            <td>{idx + 1}</td>
                                            <td>{historyItem.cltrHstrNo || '-'}</td>
                                            <td>{historyItem.feeRate || '-'}</td>
                                            <td>{formatCurrency(historyItem.minBidPrc)}</td>
                                            <td>{historyItem.pbctBegnDtm || '-'}</td>
                                            <td>{historyItem.pbctClsDtm || '-'}</td>
                                            <td>{historyItem.pbctCltrStatNm || '-'}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </div>
                )}


            </div>
        </div>
    );
};

export default DetailModal;