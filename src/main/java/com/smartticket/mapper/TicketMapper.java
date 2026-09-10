package com.smartticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartticket.entity.Ticket;

/**
 * 工单 Mapper。
 * 继承 BaseMapper 后，单表的增删改查（insert / deleteById / updateById / selectById / selectList）
 * 全都自动有了，一行 SQL 都不用写。
 * 复杂查询再自己加方法，配 XML 或 @Select 注解。
 */
public interface TicketMapper extends BaseMapper<Ticket> {
}
