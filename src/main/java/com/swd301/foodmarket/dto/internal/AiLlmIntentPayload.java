package com.swd301.foodmarket.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiLlmIntentPayload {

    /** Intent chính: BUY_ITEM, SEARCH_ITEM, LIST_CATALOG, ... */
    private String intent;

    /** Tên sản phẩm/món (dùng cho SEARCH_ITEM — tìm kiếm, xem chi tiết, hỏi giá...) */
    private String productName;

    /** @deprecated dùng productName; giữ để tương thích nếu model trả keyword */
    private String keyword;

    /** Chọn option số mấy trong danh sách vừa hiện */
    private Integer selectedIndex;

    /** Danh sách món cần thêm giỏ (BUY_ITEM) */
    private List<AiLlmItemPayload> items;

    /** Chủ đề câu hỏi chung (ASK_INFORMATION), vd: "phí ship", "giao hàng" */
    private String topic;
}
