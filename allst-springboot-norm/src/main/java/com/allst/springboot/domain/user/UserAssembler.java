package com.allst.springboot.domain.user;

public final class UserAssembler {

    private UserAssembler() {
    }

    public static UserVO toVO(UserEntity entity) {
        UserVO vo = new UserVO();
        vo.setId(entity.getId());
        vo.setUsername(entity.getUsername());
        vo.setRealName(entity.getRealName());
        vo.setPhone(entity.getPhone());
        vo.setEmail(entity.getEmail());
        vo.setIdCard(entity.getIdCard());
        vo.setStatus(entity.getStatus());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
