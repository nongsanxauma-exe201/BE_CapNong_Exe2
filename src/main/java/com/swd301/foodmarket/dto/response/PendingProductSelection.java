package com.swd301.foodmarket.dto.response;

import com.swd301.foodmarket.enums.AiPendingSelectionMode;
import com.swd301.foodmarket.dto.internal.AiCatalogOption;
import com.swd301.foodmarket.dto.request.AiItemCommand;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingProductSelection {

    private Integer userId;
    private AiItemCommand originalItem;
    private List<AiCatalogOption> options;
    private AiPendingSelectionMode mode;
    private LocalDateTime createdAt;
}
