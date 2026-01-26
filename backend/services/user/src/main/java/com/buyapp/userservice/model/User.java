package com.buyapp.userservice.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    private String id;

    @NotBlank(message = "Name can't be empty")
    @Size(min = 2, max = 50)
    private String name;

    @Email
    @NotBlank(message = "Email can't be empty")
    private String email;

    @NotBlank(message = "Password can't be empty")
    @Size(min = 3)
    private String password;

    @NotNull
    private Role role;

    private String avatar; // Optional avatar image path for sellers

    public User(String id, String name, String email, String password, Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public User(String id, String name, String email, String password, Role role, String avatar) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.avatar = avatar;
    }

    // Custom methods for role handling
    public String getRole() {
        return role.getValue();
    }

    public void setRole(String role) {
        this.role = Role.fromString(role);
    }

    public Role getRoleEnum() {
        return role;
    }
}
