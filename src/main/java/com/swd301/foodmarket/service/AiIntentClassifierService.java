package com.swd301.foodmarket.service;

import com.swd301.foodmarket.dto.request.AiDetectedIntent;

public interface AiIntentClassifierService {

    AiDetectedIntent classify(String message);
}
