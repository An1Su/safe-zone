package com.buyapp.mediaservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "avatars")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Avatar {
    @Id
    private String id;

    @NotBlank(message = "Image path can't be empty")
    private String imagePath;

    @NotBlank(message = "User ID can't be null")
    @Indexed(unique = true)
    private String userId;

    private String fileName;
    private String contentType;
    private Long fileSize;
}
