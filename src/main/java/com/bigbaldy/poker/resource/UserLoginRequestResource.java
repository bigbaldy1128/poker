package com.bigbaldy.poker.resource;

import com.bigbaldy.poker.model.type.ThirdPartyUserType;
import lombok.Data;

@Data
public class UserLoginRequestResource {
    private String token;
    private ThirdPartyUserType thirdPartyUserType;
}
