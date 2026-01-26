package com.buyapp.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String id;

    @NotBlank
    @Size(min = 2, max = 50)
    private String name;

    @Email
    @NotBlank
    private String email;

    // Only used for registration
    @Size(min = 3)
    private String password;

    // Role: client or seller
    @NotBlank
    private String role;

    private String avatar; // Optional avatar for sellers
}
