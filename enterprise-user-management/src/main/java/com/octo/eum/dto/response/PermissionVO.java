package com.octo.eum.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限视图对象
 *
 * @author octo
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PermissionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 权限ID
     */
    private Long id;

    /**
     * 父级ID
     */
    private Long parentId;

    /**
     * 权限编码
     */
    private String code;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 类型 1-目录 2-菜单 3-按钮
     */
    private Integer type;

    /**
     * 类型描述
     */
    private String typeDesc;

    /**
     * 路由路径
     */
    private String path;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 图标
     */
    private String icon;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 是否可见 0-隐藏 1-显示
     */
    private Integer visible;

    /**
     * 状态 0-禁用 1-正常
     */
    private Integer status;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 子权限列表
     */
    private List<PermissionVO> children;

    /**
     * 获取类型描述
     */
    public String getTypeDesc() {
        if (type == null) return "未知";
        return switch (type) {
            case 1 -> "目录";
            case 2 -> "菜单";
            case 3 -> "按钮";
            default -> "未知";
        };
    }
}

