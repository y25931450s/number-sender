package com.xiaoying.base.numbersender.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.xiaoying.base.numbersender.api.service.CheckService;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author:lijin,E-mail:jin.li@quvideo.com>
 * @created:2018/10/31
 * @function:
 */
@Path("/healthycheck")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Service(protocol = {"dubbo","rest"}, timeout = 3000)
public class CheckServiceImpl implements CheckService {
    @Path("/checkapp")
    @GET
    @Override
    public String check() {
        return "OK";
    }
}
