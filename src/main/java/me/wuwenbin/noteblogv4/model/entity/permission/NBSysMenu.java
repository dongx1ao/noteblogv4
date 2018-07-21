package me.wuwenbin.noteblogv4.model.entity.permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

/**
 * created by Wuwenbin on 2018/7/20 at 21:23
 */
@Entity
@Data
@Table(name = "sys_menu")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NBSysMenu implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false, length = 11)
    private Long id;

    @Column(nullable = false, length = 11)
    private Long parentId;

    private String name;

    @Column(nullable = false, length = 11)
    private Long resourceId;

    private String icon;

    @Enumerated(EnumType.STRING)
    private MenuType type;

    @Column(nullable = false, length = 11)
    private long roleId;

    @Column(length = 1, columnDefinition = "tinyint(1)", nullable = false)
    @Builder.Default
    private Boolean enable = Boolean.TRUE;

    private String remark;

    /**
     * 菜单类型
     * 目前只支持两级菜单
     */
    public enum MenuType {

        /**
         * 根节点
         */
        ROOT,

        /**
         * 非叶节点，即父节点
         */
        PARENT,

        /**
         * 叶子节点，子节点，无下级的
         */
        LEAF;
    }
}