package com.innoshare.model.request;
import java.sql.Timestamp;

import lombok.Data;

@Data
public class UserRequest {
    private Integer userId;
    private String username;
    private String password;
}