package com.hmdp.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class UserDTO {
    private Long id;
    private String nickName;
    private String icon;
}
