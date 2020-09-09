package com.xiaoying.base.numbersender.domain.segment;

import com.xiaoying.base.numbersender.api.constant.NumberSenderError;
import com.xiaoying.base.numbersender.domain.bo.Segment;
import com.xiaoying.base.numbersender.domain.bo.SegmentBuffer;
import com.xiaoying.base.numbersender.infrastructure.dao.SegmentAllocDAO;
import com.xiaoying.base.numbersender.infrastructure.dataobject.SegmentAllocDO;
import com.xiaoying.common.service.framework.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;


/**
 * @author:lijin,E-mail:jin.li@quvideo.com>
 * @created:2019/3/12
 * @function: segment 发号器实现
 */
@Component
public class SegmentService implements InitializingBean {
    private Logger LOGGER = LoggerFactory.getLogger(SegmentService.class);

    @Value("${number.sender.segmentSize}")
    private Integer segmentSize;
    @Resource
    private SegmentAllocDAO segmentAllocDAO;
    /**
     * 是否初始化完成
     */
    private volatile boolean initialized;

    private Map<String, SegmentBuffer> segmentBufferMap = new ConcurrentHashMap<>(32);

    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(2,
            10,
            1000L,
            TimeUnit.SECONDS,
            new LinkedBlockingDeque<>(100),
            new SegmentThreadFactory(), new ThreadPoolExecutor.DiscardOldestPolicy()
    );

    @Override
    public void afterPropertiesSet() throws Exception {
        //初始加载
        updateAllSegmentsFromDB();

        //每分钟更新
        updateAllSegmentsFromDBEveryMinute();
        initialized = true;
    }

    private void updateAllSegmentsFromDBEveryMinute() {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> updateAllSegmentsFromDB(),
                1000,
                2 * 60 * 1000, TimeUnit.MILLISECONDS
        );
    }

    private void updateAllSegmentsFromDB() {
        List<SegmentAllocDO> segmentAllocDOList = segmentAllocDAO.loadAll();
        if (!CollectionUtils.isEmpty(segmentAllocDOList)) {
            for (SegmentAllocDO segmentAllocDO : segmentAllocDOList) {
                if (!segmentBufferMap.keySet().contains(segmentAllocDO.getTag())) {
                    SegmentBuffer segmentBuffer = new SegmentBuffer();
                    initBuffer(segmentBuffer, segmentAllocDO);
                    segmentBufferMap.putIfAbsent(segmentAllocDO.getTag(), segmentBuffer);
                }
            }
        }
    }

    /**
     * 初始化segmentBuffer，更新当前号段
     * @param segmentBuffer
     * @param segmentAllocDO
     */
    private void initBuffer(SegmentBuffer segmentBuffer, SegmentAllocDO segmentAllocDO) {
        Segment segment = segmentBuffer.getCurrent();
        updateSegmentFromDB(segment, segmentAllocDO);
        segmentBuffer.setInitOk(true);
    }

    /**
     * 从数据库中取号，直到取号成功，利用数据库乐观锁
     * @param segment
     * @param segmentAllocDO
     */
    private void updateSegmentFromDB(Segment segment, SegmentAllocDO segmentAllocDO) {
        while (true) {
            long preMaxId = segmentAllocDO.getMaxId();
            long maxId = preMaxId + segmentSize * segmentAllocDO.getStep();
            int effectRow = segmentAllocDAO.updateMaxId(segmentAllocDO.getTag(), preMaxId, maxId);
            if (effectRow > 0) {
                //取号成功
                LOGGER.info("segment update success,tag:{},pre MaxId:{},current MaxId:{}",segmentAllocDO.getTag(),preMaxId,maxId);
                segment.setMax(maxId);
                segment.setStep(segmentAllocDO.getStep());
                segment.setValue(new AtomicLong(preMaxId));
                break;
            }
            //在目前号段基础再继续取号
            LOGGER.info("segment update failed,tag:{},pre MaxId:{},current MaxId:{},then retry...",segmentAllocDO.getTag(),preMaxId,maxId);
            segmentAllocDO = segmentAllocDAO.getByTag(segmentAllocDO.getTag());
        }
    }

    /**
     * 发号
     * @param tag
     * @return
     */
    public Long getID(String tag) {
        if (StringUtils.isEmpty(tag)) {
            throw new BizException(NumberSenderError.SEGMENT_TAG_NULL);
        }
        if (!segmentBufferMap.keySet().contains(tag)) {
            LOGGER.error("[segment] biz tag is not exist,tag:{}",tag);
            throw new BizException(NumberSenderError.SEGMENT_NO_TAG);
        }

        if (!initialized) {
            throw new BizException(NumberSenderError.SEGMENT_NOT_INIT);
        }

        SegmentBuffer segmentBuffer = segmentBufferMap.get(tag);
        while (true) {
            Lock rl = segmentBuffer.rLock();
            rl.lock();
            try {
                //如果下一个没准备好，将下一个初始化
                if (!segmentBuffer.isNextReady() && segmentBuffer.getThreadRunning().compareAndSet(false, true)) {
                    threadPoolExecutor.submit(() -> {
                        Segment nextSegment = segmentBuffer.getNext();
                        boolean isUpdateOK = false;
                        try {
                            SegmentAllocDO segmentAllocDO = segmentAllocDAO.getByTag(tag);
                            updateSegmentFromDB(nextSegment, segmentAllocDO);
                            isUpdateOK = true;
                        } catch (Exception e) {
                            LOGGER.error("update next segment fail,tag:{}", tag);
                        } finally {

                            if (isUpdateOK) {
                                segmentBuffer.wLock().lock();
                                segmentBuffer.setNextReady(true);
                                segmentBuffer.getThreadRunning().set(false);
                                segmentBuffer.wLock().unlock();
                            } else {
                                segmentBuffer.getThreadRunning().set(false);
                            }
                        }
                    });
                }
                Segment segment = segmentBuffer.getCurrent();
                Long id = segment.increase();
                if (id <= segment.getMax()) {
                    return id;
                }
            } finally {
                rl.unlock();
            }

            Lock wl = segmentBuffer.wLock();
            wl.lock();
            try {
                if (segmentBuffer.isNextReady()) {
                    //若下一号段已经准备好，直接切换
                    segmentBuffer.switchPos();
                    segmentBuffer.setNextReady(false);
                } else {
                    //不然，则更新一下号段，然后切换
                    Segment segment = segmentBuffer.getNext();
                    SegmentAllocDO segmentAllocDO = segmentAllocDAO.getByTag(tag);
                    updateSegmentFromDB(segment, segmentAllocDO);
                    segmentBuffer.switchPos();
                    segmentBuffer.setNextReady(false);
                }
            } finally {
                wl.unlock();
            }
        }
    }

    public static class SegmentThreadFactory implements ThreadFactory {
        private AtomicLong threadCount = new AtomicLong();

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "next_segment_update_thread_" + threadCount.incrementAndGet());
        }
    }
}
