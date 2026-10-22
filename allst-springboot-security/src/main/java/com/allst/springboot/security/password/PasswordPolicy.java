package com.allst.springboot.security.password;

import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.exception.BizException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 口令复杂度。BCrypt 只取前 72 个字符，所以上限也卡在 72。见 docs/05-password-management.md。
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 72;

    private PasswordPolicy() {
    }

    public static void check(String username, String raw, List<String> denylist) {
        List<String> errors = new ArrayList<>();
        if (raw == null || raw.length() < MIN_LENGTH || raw.length() > MAX_LENGTH) {
            errors.add("长度必须在 " + MIN_LENGTH + " 到 " + MAX_LENGTH + " 之间");
        }
        if (raw != null) {
            if (raw.chars().noneMatch(Character::isLowerCase)) {
                errors.add("至少包含一个小写字母");
            }
            if (raw.chars().noneMatch(Character::isUpperCase)) {
                errors.add("至少包含一个大写字母");
            }
            if (raw.chars().noneMatch(Character::isDigit)) {
                errors.add("至少包含一个数字");
            }
            if (raw.chars().noneMatch(ch -> !Character.isLetterOrDigit(ch))) {
                errors.add("至少包含一个符号");
            }
            if (username != null && raw.toLowerCase(Locale.ROOT).contains(username.toLowerCase(Locale.ROOT))) {
                errors.add("不能包含用户名");
            }
            if (denylist != null) {
                for (String weak : denylist) {
                    if (weak != null && weak.equalsIgnoreCase(raw)) {
                        errors.add("命中了禁用口令");
                        break;
                    }
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new BizException(ResultCode.PASSWORD_POLICY, 400, String.join("；", errors));
        }
    }

    public static void checkDisplayName(String displayName) {
        if (displayName == null) {
            return;
        }
        for (int i = 0; i < displayName.length(); i++) {
            char ch = displayName.charAt(i);
            if (ch == '<' || ch == '>' || ch == '"' || ch == '\'') {
                throw new BizException(ResultCode.VALIDATION, 400, "显示名不能包含引号或尖括号");
            }
        }
    }
}
