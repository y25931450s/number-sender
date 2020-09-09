package com.xiaoying.base.numbersender.domain.bo;

import lombok.Data;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author:lijin,E-mail:jin.li@quvideo.com>
 * @created:2019/3/12
 * @function: 号段
 */
@Data
public class Segment {
    private AtomicLong value = new AtomicLong(0);
    private volatile long max;
    private volatile int step;

    private transient SegmentBuffer buffer;

    public Segment(SegmentBuffer buffer) {
        this.buffer = buffer;
    }

    /**
     * 增加一个
     * @return
     */
    public long increase() {
        return value.addAndGet(step);
    }

    @Override
    public String toString() {
        return "{" +
                "value=" + value +
                ", max=" + max +
                ", step=" + step +
                '}';
    }
}
