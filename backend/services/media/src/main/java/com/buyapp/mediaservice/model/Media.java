package com.buyapp.mediaservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "media")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Media {
    @Id
    private String id;

    @NotBlank(message = "Image path can't be empty")
    private String imagePath;

    @NotNull(message = "Product ID can't be null")
    @Field("productId")
    private String productId;

    private String fileName;
    private String contentType;
    private Long fileSize;
}
