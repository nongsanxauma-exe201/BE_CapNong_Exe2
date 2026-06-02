package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.WalletCreationRequest;
import com.swd301.foodmarket.dto.request.WithdrawRequestCreationRequest;
import com.swd301.foodmarket.dto.response.WalletResponse;
import com.swd301.foodmarket.dto.response.WithdrawRequestResponse;
import com.swd301.foodmarket.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface WalletService {

    WalletResponse getMyWallet();

    WalletResponse getMyShipperWallet();

    WithdrawRequestResponse createWithdrawRequest(WithdrawRequestCreationRequest request);

    WithdrawRequestResponse createShipperWithdrawRequest(WithdrawRequestCreationRequest request);

    WithdrawRequestResponse rejectWithdraw(Integer requestId, String adminNote,List<MultipartFile> files);

    List<WithdrawRequestResponse> getPendingWithdrawRequests();

    List<WithdrawRequestResponse> getMyWithdrawRequests();

    List<WithdrawRequestResponse> getMyShipperWithdrawRequests();

    List<WithdrawRequestResponse> getAllWithdrawRequests();

    WithdrawRequestResponse createWithdrawQr(Integer requestId);

    WithdrawRequestResponse confirmWithdrawSuccess(Integer requestId, String adminNote, List<MultipartFile> multipartFiles);
}