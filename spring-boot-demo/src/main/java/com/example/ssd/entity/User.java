package com.example.ssd.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author zms
 * @since 2024-05-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("gender")
    private Boolean gender;

    @TableField("phone_number")
    private String phoneNumber;

    @TableField("id_card")
    private String idCard;

    @TableField("unit_id")
    private Long unitId;

    @TableField("unit_name")
    private String unitName;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDate updateTime;

    @TableField("user_type")
    private Integer userType;

    @TableField("aa")
    private String aa;


}
