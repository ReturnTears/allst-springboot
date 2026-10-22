package com.allst.springboot.controller;

import com.allst.springboot.common.api.Result;
import com.allst.springboot.common.api.ResultCode;
import com.allst.springboot.common.exception.BizException;
import com.allst.springboot.config.NormProperties;
import com.allst.springboot.crypto.AesCipher;
import com.allst.springboot.crypto.SignUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.UUID;

/**
 * 演示环境用来计算签名和观察密文。它掌握服务端密钥，生产必须关闭。
 */
@RestController
@RequestMapping("/api/dev")
@Tag(name = "开发辅助", description = "生产环境设置 norm.dev-tools-enabled=false")
public class DevToolController {

    private final NormProperties properties;

    public DevToolController(NormProperties properties) {
        this.properties = properties;
    }

    @PostMapping("/sign")
    @Operation(summary = "计算签名", description = "body、query 必须与真实请求完全一致。响应不含密钥。")
    public Result<SignTicket> sign(@Valid @RequestBody SignBuildCommand command) {
        assertEnabled();
        String appId = command.getAppId() == null || command.getAppId().isBlank() ? "demo-app" : command.getAppId();
        String secret = properties.getSign().getApps().get(appId);
        if (secret == null || secret.isBlank()) {
            throw new BizException(ResultCode.SIGN_APP);
        }
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String body = command.getBody() == null ? "" : command.getBody();
        String canonical = SignUtils.canonical(command.getMethod(), command.getPath(), command.getQuery(), body,
                appId, timestamp, nonce, command.getIdempotentKey(), command.getOperator());
        SignTicket ticket = new SignTicket();
        ticket.setAppId(appId);
        ticket.setTimestamp(timestamp);
        ticket.setNonce(nonce);
        ticket.setIdempotentKey(command.getIdempotentKey());
        ticket.setOperator(command.getOperator());
        ticket.setSign(SignUtils.sign(canonical, secret));
        ticket.setCanonical(canonical);
        return Result.ok(ticket);
    }

    @PostMapping("/crypto/encrypt")
    @Operation(summary = "加密一段文本", description = "用于观察 AES-GCM 密文。业务字段请走用户接口，由转换器自动加密。")
    public Result<String> encrypt(@Valid @RequestBody PlainTextCommand command) {
        assertEnabled();
        return Result.ok(AesCipher.encrypt(command.getPlainText(), properties.getCrypto().getAesKey()));
    }

    @PostMapping("/crypto/decrypt")
    @Operation(summary = "解密一段文本", description = "演示接口。生产关闭后，密钥不应离开服务端。")
    public Result<String> decrypt(@Valid @RequestBody CipherTextCommand command) {
        assertEnabled();
        return Result.ok(AesCipher.decrypt(command.getCipherText(), properties.getCrypto().getAesKey()));
    }

    private void assertEnabled() {
        if (!properties.isDevToolsEnabled()) {
            throw new BizException(ResultCode.API_NOT_FOUND, "开发辅助接口未开放");
        }
    }

    @Data
    public static class SignBuildCommand {
        @NotBlank(message = "方法不能为空")
        @Pattern(regexp = "(?i)GET|POST|PUT|PATCH|DELETE", message = "方法只能是 GET、POST、PUT、PATCH、DELETE")
        private String method;
        @NotBlank(message = "路径不能为空")
        @Pattern(regexp = "^/\\S*$", message = "路径必须以 / 开头且不能包含空白")
        private String path;
        private String query;
        @Schema(description = "原始 JSON 字符串，必须与即将发送的报文字节一致")
        private String body;
        private String appId;
        private String idempotentKey;
        private String operator;
    }

    @Data
    public static class SignTicket {
        private String appId;
        private String timestamp;
        private String nonce;
        private String idempotentKey;
        private String operator;
        private String sign;
        private String canonical;
    }

    @Data
    public static class PlainTextCommand {
        @NotBlank(message = "明文不能为空")
        @Size(max = 2000, message = "明文长度超出限制")
        private String plainText;
    }

    @Data
    public static class CipherTextCommand {
        @NotBlank(message = "密文不能为空")
        private String cipherText;
    }
}
