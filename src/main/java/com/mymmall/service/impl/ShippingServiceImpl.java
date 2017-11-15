package com.mymmall.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mymmall.common.ServerResponse;
import com.mymmall.dao.ShippingMapper;
import com.mymmall.pojo.Shipping;
import com.mymmall.service.IShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service(value = "iShippingService")
public class ShippingServiceImpl implements IShippingService {

    @Autowired
    private ShippingMapper shippingMapper;

    public ServerResponse add(Integer userId,Shipping shipping){
        shipping.setUserId(userId);
        int rowCount = shippingMapper.insert(shipping);
        if (rowCount > 0){
            Map result = Maps.newHashMap();
            result.put("shippingId",shipping.getId());
            return ServerResponse.createBySuccessMessage("新建地址成功",result);
        }
        return ServerResponse.createByErrorMessage("新建地址失败");
    }

    @Override
    public ServerResponse delete(Integer userId, Integer shippingId) {
        int rowCount = shippingMapper.deleteByUserIdAndShippingId(userId,shippingId);
        if (rowCount>0){
            return ServerResponse.createBySuccessMessage("地址删除成功");
        }
        return ServerResponse.createByErrorMessage("地址删除失败");
    }

    @Override
    public ServerResponse update(Integer userId, Shipping shipping) {
        shipping.setUserId(userId);
        int rowCount = shippingMapper.updateByUserId(shipping);
        if (rowCount>0){
            return ServerResponse.createBySuccessMessage("地址更新成功");
        }
        return ServerResponse.createByErrorMessage("更新地址失败");
    }

    @Override
    public ServerResponse select(Integer userId, Integer shippingId) {
        Shipping shipping = shippingMapper.selectByUserIdAndShippingId(userId,shippingId);
        if (shipping == null){
            return ServerResponse.createByErrorMessage("获取地址详细信息失败");
        }
        return ServerResponse.createBySuccessMessage("获取地址详细信息成功",shipping);
    }

    @Override
    public ServerResponse<PageInfo> getList(Integer userId, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        List<Shipping> shippingList = shippingMapper.selectByUserId(userId);
        PageInfo pageInfo = new PageInfo(shippingList);
        return ServerResponse.createBySuccessMessage(pageInfo);
    }
}
