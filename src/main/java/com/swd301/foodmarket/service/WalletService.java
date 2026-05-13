package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.WalletCreationRequest;
import com.swd301.foodmarket.dto.request.WithdrawRequestCreationRequest;
import com.swd301.foodmarket.dto.response.WalletResponse;
import com.swd301.foodmarket.dto.response.WithdrawRequestResponse;

import java.util.List;

public interface WalletService {

    WalletResponse getMyWallet();

    WalletResponse getMyShipperWallet();

    WithdrawRequestResponse createWithdrawRequest(WithdrawRequestCreationRequest request);

    WithdrawRequestResponse createShipperWithdrawRequest(WithdrawRequestCreationRequest request);

    WithdrawRequestResponse rejectWithdraw(Integer requestId, String adminNote);

    List<WithdrawRequestResponse> getPendingWithdrawRequests();

    List<WithdrawRequestResponse> getMyWithdrawRequests();

    List<WithdrawRequestResponse> getMyShipperWithdrawRequests();

    List<WithdrawRequestResponse> getAllWithdrawRequests();

    WithdrawRequestResponse createWithdrawQr(Integer requestId);

    WithdrawRequestResponse confirmWithdrawSuccess(Integer requestId, String adminNote);
}