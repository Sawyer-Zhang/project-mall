package com.mymmall.service;

import com.github.pagehelper.PageInfo;
import com.mymmall.common.ServerResponse;
import com.mymmall.pojo.Shipping;

public interface IShippingService {
    ServerResponse add(Integer userId, Shipping shipping);
    ServerResponse delete(Integer userId,Integer shippingId);
    ServerResponse update(Integer userId,Shipping shipping);
    ServerResponse select(Integer userId,Integer shippingId);
    ServerResponse<PageInfo> getList(Integer userId,int pageNum,int pageSize);
}
