package com.allst.springboot.extension.imports;

/**
 * ImportSelector 导入的模块标记。
 */
public class AdvsImportedMarker {

    private final String message;

    public AdvsImportedMarker(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
