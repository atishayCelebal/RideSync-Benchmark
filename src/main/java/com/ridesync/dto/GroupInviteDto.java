package com.ridesync.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class GroupInviteDto {
    
    @NotNull
    private Long groupId;
    
    @NotBlank
    @Email
    private String email;
    
    private String message;
    
    // Constructors
    public GroupInviteDto() {}
    
    public GroupInviteDto(Long groupId, String email, String message) {
        this.groupId = groupId;
        this.email = email;
        this.message = message;
    }
    
    // Getters and Setters
    public Long getGroupId() {
        return groupId;
    }
    
    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public String toString() {
        return "GroupInviteDto{" +
                "groupId=" + groupId +
                ", email='" + email + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
