package com.mymmall.service;

import com.github.pagehelper.PageInfo;
import com.mymmall.common.ServerResponse;
import com.mymmall.vo.OrderVo;

import java.util.Map;

public interface IOrderService {
    ServerResponse pay(Long orderNo, Integer userId, String path);
    ServerResponse alipayCallback(Map<String,String> params);
    ServerResponse queryOrderPayStatus(Long orderNo,Integer userId);
    ServerResponse createOrder(Integer userId,Integer shippingId);
    ServerResponse<String> cancelOrder(Integer userId,Long orderNo);
    ServerResponse getOrderCartProduct(Integer userId);
    ServerResponse<OrderVo> getOrderDetail(Integer userId,Long orderNo);
    ServerResponse<PageInfo> getOrderList(Integer userId, int pageNum, int pageSize);

    ServerResponse<PageInfo> getManageOrderList(int pageNum,int pageSize);
    ServerResponse<OrderVo> getManageOrderDetail(Long orderNo);
    ServerResponse<PageInfo> getManageOrderSearch(Long orderNo,int pageNum,int pageSize);
    ServerResponse<String> manageSendGoods(Long orderNo);
}
