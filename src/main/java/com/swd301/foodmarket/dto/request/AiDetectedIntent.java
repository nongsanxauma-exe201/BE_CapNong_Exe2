package com.swd301.foodmarket.dto.request;

import com.swd301.foodmarket.enums.AiAccountActionType;
import com.swd301.foodmarket.enums.AiIntentType;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiDetectedIntent {
    private AiIntentType intent;
    private String rawMessage;
    private String keyword;
    private Integer selectedIndex;
    private List<AiItemCommand> items;
    private AiAccountActionType accountAction;
    private Integer orderId;
}