package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.ReturnActionRequest;
import com.swd301.foodmarket.dto.request.ReturnCreateRequest;
import com.swd301.foodmarket.dto.response.ReturnActionResponse;
import com.swd301.foodmarket.entity.ReturnRequest;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ReturnRequestService {
    ReturnRequest createReturnRequest(ReturnCreateRequest request, MultipartFile evidence);
    ReturnActionResponse shopAction(Integer requestId, ReturnActionRequest action);
    ReturnActionResponse adminAction(Integer requestId, ReturnActionRequest action);
    ReturnRequest dispute(Integer requestId, String reason);
    
    List<ReturnRequest> getMyReturns();
    List<ReturnRequest> getShopReturns();
    List<ReturnRequest> getDisputes();
    ReturnRequest getById(Integer id);
    ReturnActionResponse checkPayoutStatus(Integer requestId);
    ReturnActionResponse autoCheckByOrderCode(Long orderCode);
}
