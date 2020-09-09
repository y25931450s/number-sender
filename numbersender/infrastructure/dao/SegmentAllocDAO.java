package com.xiaoying.base.numbersender.infrastructure.dao;

import com.xiaoying.base.numbersender.infrastructure.dataobject.SegmentAllocDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author:lijin,E-mail:jin.li@quvideo.com>
 * @created:2019/3/12
 * @function: 号段分配 DAO
 */
public interface SegmentAllocDAO {
    /**
     * 获取当前号段
     * @param tag
     * @return
     */
    SegmentAllocDO getByTag(@Param("tag") String tag);

    /**
     * 更新号段
     * @param tag
     * @param preMaxId
     * @param maxId
     * @return
     */
    int updateMaxId(@Param("tag") String tag, @Param("preMaxId")Long preMaxId, @Param("maxId")Long maxId);

    /**
     * 获取所有的tag
     * @return
     */
    List<SegmentAllocDO> loadAll();
}
