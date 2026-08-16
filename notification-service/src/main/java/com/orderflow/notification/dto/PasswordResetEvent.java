package com.orderflow.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// @JsonIgnoreProperties ensures any future fields added by auth-service
// won't break deserialization here.
@JsonIgnoreProperties(ignoreUnknown = true)
public class PasswordResetEvent {
    private String eventType;
    private Long userId;
    private String userEmail;
    private String username;
    private String resetLink;
    private long expiresInMinutes;
    private String timestamp;   // published by auth-service; not used for sending but must be accepted

    public String getEventType()           { return eventType; }
    public void setEventType(String t)     { this.eventType = t; }
    public Long getUserId()                { return userId; }
    public void setUserId(Long id)         { this.userId = id; }
    public String getUserEmail()           { return userEmail; }
    public void setUserEmail(String e)     { this.userEmail = e; }
    public String getUsername()            { return username; }
    public void setUsername(String u)      { this.username = u; }
    public String getResetLink()           { return resetLink; }
    public void setResetLink(String l)     { this.resetLink = l; }
    public long getExpiresInMinutes()      { return expiresInMinutes; }
    public void setExpiresInMinutes(long m){ this.expiresInMinutes = m; }
    public String getTimestamp()           { return timestamp; }
    public void setTimestamp(String t)     { this.timestamp = t; }
}
