package com.allst.springboot.warehouse.api;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record CommandRequest(
        @NotBlank String action,
        Map<String, Object> params
) {
}
