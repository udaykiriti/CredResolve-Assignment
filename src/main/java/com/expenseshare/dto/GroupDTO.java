package com.expenseshare.dto;

import lombok.*;

/**
 * DTO for group creation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupDTO {
    private String name;
    private String description;
    private String memberEmails; // Comma-separated emails
}
