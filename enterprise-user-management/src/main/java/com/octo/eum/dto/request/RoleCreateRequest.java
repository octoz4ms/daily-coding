package com.octo.eum.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 角色创建请求DTO
 *
 * @author octo
 */
@Data
public class RoleCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色编码
     */
    @NotBlank(message = "角色编码不能为空")
    @Size(max = 32, message = "角色编码最大长度为32")
    private String code;

    /**
     * 角色名称
     */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 32, message = "角色名称最大长度为32")
    private String name;

    /**
     * 角色描述
     */
    @Size(max = 255, message = "角色描述最大长度为255")
    private String description;

    /**
     * 排序
     */
    private Integer sort = 0;

    /**
     * 状态 0-禁用 1-正常
     */
    private Integer status = 1;

    /**
     * 权限ID列表
     */
    private List<Long> permissionIds;
}

