package com.octo.ssd.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 权限DTO（用于创建和更新权限）
 *
 * @author octo
 */
@Data
public class PermissionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 权限ID（更新时使用）
     */
    private Long id;

    /**
     * 权限编码
     */
    @NotBlank(message = "权限编码不能为空")
    @Size(max = 100, message = "权限编码长度不能超过100个字符")
    private String permCode;

    /**
     * 权限名称
     */
    @NotBlank(message = "权限名称不能为空")
    @Size(max = 50, message = "权限名称长度不能超过50个字符")
    private String permName;

    /**
     * 权限描述
     */
    @Size(max = 200, message = "描述长度不能超过200个字符")
    private String description;
}









