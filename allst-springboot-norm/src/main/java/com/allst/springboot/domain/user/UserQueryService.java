package com.allst.springboot.domain.user;

import com.allst.springboot.common.api.PageData;
import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.common.guard.SizeGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 只读用户。和写入服务分开，查询条件变化时不用改创建逻辑。
 * 对应规范：18 职责单一，10 限制记录条数。
 */
@Service
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserRepository userRepository;
    private final SizeGuard sizeGuard;

    public UserQueryService(UserRepository userRepository, SizeGuard sizeGuard) {
        this.userRepository = userRepository;
        this.sizeGuard = sizeGuard;
    }

    public PageData<UserVO> page(UserPageQuery query) {
        int page = sizeGuard.pageIndex(query.getPage());
        int size = sizeGuard.pageSize(query.getSize());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<UserEntity> result;
        if (query.getUsername() == null || query.getUsername().isBlank()) {
            result = userRepository.findAll(pageable);
        } else {
            result = userRepository.findByUsernameContaining(query.getUsername().trim(), pageable);
        }
        List<UserVO> records = result.getContent().stream().map(UserAssembler::toVO).collect(Collectors.toList());
        return new PageData<>(result.getTotalElements(), page, size, records);
    }

    public UserVO get(Long id) {
        return UserAssembler.toVO(require(id));
    }

    UserEntity require(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BizException(ResultCode.NOT_FOUND, "用户不存在"));
    }
}
