package com.xiaoying.base.numbersender.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.xiaoying.api.common.response.PlainResult;
import com.xiaoying.base.numbersender.api.service.NumberSenderService;
import com.xiaoying.base.numbersender.domain.segment.SegmentService;
import com.xiaoying.base.numbersender.domain.snowflake.SnowflakeService;
import com.xiaoying.common.service.framework.ServiceAspectAnnotation;

import javax.annotation.Resource;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * @author:lijin,E-mail:jin.li@quvideo.com>
 * @created:2019/3/13
 * @function: 发号器服务实现类
 */
@Service(protocol = {"dubbo","rest"} ,timeout = 3000)
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Produces({MediaType.APPLICATION_JSON})
@Path("id")
public class NumberSenderServiceImpl implements NumberSenderService {
    @Resource
    private SegmentService segmentService;

    @Resource
    private SnowflakeService snowflakeService;

    @Override
    @ServiceAspectAnnotation
    @GET
    @Path("segment")
    public PlainResult<Long> getIDBySegment(@QueryParam("tag") String tag) {
        PlainResult<Long> result = new PlainResult<>();
        long id = segmentService.getID(tag);
        result.setData(id);
        return result;
    }

    @Override
    @ServiceAspectAnnotation
    @Path("snowflake")
    @GET
    public PlainResult<Long> getIDBySnowFlake() {
        PlainResult<Long> result = new PlainResult<>();
        long id = snowflakeService.getID();
        result.setData(id);
        return result;
    }
}
