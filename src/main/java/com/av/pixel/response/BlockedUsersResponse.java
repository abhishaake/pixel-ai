package com.av.pixel.response;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class BlockedUsersResponse {

    List<BlockedUser> blockedUsers;

    @Data
    @Accessors(chain = true)
    public static class BlockedUser {
        String name;
        String userCode;
    }
}
