package com.buyapp.common.security;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Document(collection = "blacklisted_tokens")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BlacklistedToken {
    @Id
    private String id;

    private String token;
    private Date expiryDate;
}
