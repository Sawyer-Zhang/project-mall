package com.mymmall.service;

import com.mymmall.common.ServerResponse;
import com.mymmall.vo.CartVo;

public interface ICartService {
    ServerResponse<CartVo> add(Integer userId, Integer productId, Integer count);
    ServerResponse<CartVo> update(Integer userId,Integer productId,Integer count);
    ServerResponse<CartVo> deleteProduct(Integer userId,String productIds);
    ServerResponse<CartVo> getList(Integer userId);
    ServerResponse<CartVo> selectOrUnSelect(Integer userId,Integer productId,Integer checked);
    ServerResponse<Integer> getCartProductCount(Integer userId);
}
