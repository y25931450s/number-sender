package com.xiaoying.base.numbersender.domain.snowflake;

import com.xiaoying.base.numbersender.api.constant.NumberSenderError;
import com.xiaoying.common.service.framework.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author:lijin,E-mail:jin.li@quvideo.com>
 * @created:2019/3/12
 * @function: snowflake算法生成
 */
@Component
public class SnowflakeService {
    private Logger LOGGER = LoggerFactory.getLogger(SnowflakeService.class);
    @Value("${number.sender.idcno}")
    private Integer idcNo;

    /**
     * 起始时间，小影成立之时，留作纪念
     */
    private static final Long twepoch = 1340553600000L;

    /**
     * 序列号位数
     */
    private static final int SEQUENT_BITS = 12;

    /**
     * 机器号位数
     */
    private static final int WORKER_BITS = 6;

    /**
     * 机房位数
     */
    private static final int IDC_BITS = 4;

    /**
     * 机器号左位移
     */
    private static final int WORKER_SHIFT = SEQUENT_BITS;

    /**
     * 由于是容器，理论上有可能超过6位数的，但是只有把范围限制在6位，在同一时间内不可能重复
     */
    private static final long WORKER_MASK = -1L ^ (-1L << WORKER_BITS);

    /**
     * 防止同一ms内的并发超过12位数，若超过，则把时间拉到一下ms
     */
    private static final long SEQUENT_MASK = -1L ^ (-1L << SEQUENT_BITS);

    private static final int IDC_SHIFT = WORKER_SHIFT + WORKER_BITS;

    private static final int TIMESTAMP_SHIFT = IDC_SHIFT + IDC_BITS;

    private long lastTimestamp = 0L;

    private long sequence = 0L;

    @Resource
    private SnowflakeZookeeperHolder snowflakeZookeeperHolder;

    /**
     * 执行发号
     *
     * @return
     */
    public synchronized long getID() {
        //判断上次时间
        long timestamp = getCurrentTimestamp();
        if (timestamp < lastTimestamp) {
            //若发生时钟回退，则解决时钟回退问题
            if (timestamp - lastTimestamp < 5) {
                //等待
                long offset = timestamp - lastTimestamp;
                try {
                    wait(offset << 1);
                    timestamp = getCurrentTimestamp();
                    if (timestamp < lastTimestamp) {
                        LOGGER.error("[snowflake]the clock of current System is back less then 5 ms," +
                                "wait but do  not keep pace with the normal time");
                        throw new BizException(NumberSenderError.SNOWFLAKE_EXPIRE);
                    }
                } catch (InterruptedException e) {
                    throw new BizException(NumberSenderError.SNOWFLAKE_EXPIRE);
                }
            } else {
                LOGGER.error("[snowflake]the clock of current System is back more then 5 ms");
                throw new BizException(NumberSenderError.SNOWFLAKE_EXPIRE);
            }
        } else if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENT_MASK;
            if (sequence == 0L) {
                //当前ms已达到最大，等到下一ms，并发极端大的时候
                LOGGER.info("to many ids to be created in one ms,waiting until next ms");
                timestamp = untilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        long workerID = snowflakeZookeeperHolder.getWorkerID();
        long id = ((timestamp - twepoch) << TIMESTAMP_SHIFT)
                | (idcNo << IDC_SHIFT)
                | ((workerID & WORKER_MASK) << WORKER_SHIFT)
                | sequence;
        return id;
    }

    /**
     * 等到下一ms
     *
     * @param lastTimestamp
     */
    private long untilNextMillis(long lastTimestamp) {
        long currentTimestamp = getCurrentTimestamp();
        while (currentTimestamp <= lastTimestamp) {
            currentTimestamp = getCurrentTimestamp();
        }
        return currentTimestamp;
    }

    private long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
}
