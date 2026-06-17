package com.swd301.foodmarket.dto.request;

import com.swd301.foodmarket.enums.AiItemType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiItemCommand {

    private String name;

    @Builder.Default
    private Double quantity = 1.0;

    private String unit;

    @Builder.Default
    private AiItemType itemType = AiItemType.ANY;
}
