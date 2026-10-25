package com.allst.springboot.warehouse.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SimulatorScanRequest(
        @NotBlank String deviceId,
        @NotBlank String barcode,
        @NotBlank String location,
        @Min(1) Integer quantity
) {
}
