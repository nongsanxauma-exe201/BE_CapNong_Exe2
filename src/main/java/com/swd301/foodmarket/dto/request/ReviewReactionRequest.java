package com.swd301.foodmarket.dto.request;

import com.swd301.foodmarket.enums.ReactionType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewReactionRequest {
    ReactionType reactionType;
}
