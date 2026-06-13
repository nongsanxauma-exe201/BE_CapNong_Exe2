package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.response.AddressSuggestionResponse;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.service.GoongMapService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/address")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AddressController {

    GoongMapService goongMapService;

    /**
     * GET /address/suggestions?input=...
     * Trả danh sách gợi ý địa chỉ từ Goong Places AutoComplete (bias TPHCM).
     */
    @GetMapping("/suggestions")
    public ApiResponse<List<AddressSuggestionResponse>> getSuggestions(
            @RequestParam String input
    ) {
        return ApiResponse.<List<AddressSuggestionResponse>>builder()
                .result(goongMapService.getAddressSuggestions(input))
                .build();
    }
}
