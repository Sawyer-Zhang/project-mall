package com.mymmall.service;

import com.github.pagehelper.PageInfo;
import com.mymmall.common.ServerResponse;
import com.mymmall.pojo.Product;
import com.mymmall.vo.ProductDetailVo;

public interface IProductService {

    ServerResponse productSaveAndUpdate(Product product);

    ServerResponse<String> setSaleStatus(Integer productId, Integer status);

    ServerResponse<ProductDetailVo> manageProductDetail(Integer productId);

    ServerResponse<PageInfo> getList(int pageNum,int pageSize);

    ServerResponse<PageInfo> productSearch(String productName,Integer productId,int pageNum,int pageSize);

    ServerResponse<ProductDetailVo> getProductDetail(Integer productId);

    ServerResponse<PageInfo> getProductByKeywordCategory(String keyword,Integer categoryId,int pageNum,int pageSize,String orderBy);
}
