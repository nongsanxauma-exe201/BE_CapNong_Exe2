package com.swd301.foodmarket.dto.request;

import com.swd301.foodmarket.enums.BlogStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogCreationRequest {

    @NotBlank(message = "TITLE_REQUIRED")
    @Size(max = 50, message = "TITLE_TOO_LONG")
    String title;

    String content;

    String category;

    // pictureUrl sẽ được set từ MultipartFile upload, không nhận từ JSON
    BlogStatus status; // DRAFT, PUBLISHED, HIDDEN
}