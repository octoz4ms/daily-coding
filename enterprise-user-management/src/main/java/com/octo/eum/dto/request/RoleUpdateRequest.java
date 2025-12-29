package com.octo.eum.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 角色更新请求DTO
 *
 * @author octo
 */
@Data
public class RoleUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色ID
     */
    @NotNull(message = "角色ID不能为空")
    private Long id;

    /**
     * 角色名称
     */
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
    private Integer sort;

    /**
     * 状态 0-禁用 1-正常
     */
    private Integer status;

    /**
     * 权限ID列表
     */
    private List<Long> permissionIds;
}

