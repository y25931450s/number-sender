package com.xiaoying.base.numbersender.infrastructure.dataobject;

import lombok.Data;

import java.util.Date;

/**
 * @author:lijin,E-mail:jin.li@quvideo.com>
 * @created:2019/3/12
 * @function:分段分配 DO
 */

@Data
public class SegmentAllocDO {
    private String tag;

    /**
     * 当前最大号，每个机房初始值不一样，暂时定杭州1 新加坡2 美东3 法兰克福4，以此保证全球唯一
     */
    private Long maxId;

    /**
     * 自增步长
     */
    private Integer step;

    /**
     * 更新时间
     */
    private Date updateTime;
}
