package com.mymmall.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.mymmall.common.Const;
import com.mymmall.common.ResponseCode;
import com.mymmall.common.ServerResponse;
import com.mymmall.dao.CategoryMapper;
import com.mymmall.dao.ProductMapper;
import com.mymmall.pojo.Category;
import com.mymmall.pojo.Product;
import com.mymmall.service.ICategoryService;
import com.mymmall.service.IProductService;
import com.mymmall.util.DateTimeUtil;
import com.mymmall.util.PropertiesUtil;
import com.mymmall.vo.ProductDetailVo;
import com.mymmall.vo.ProductListVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service(value = "iProductService")
public class ProductServiceImpl implements IProductService {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private ICategoryService iCategoryService;
    @Override
    public ServerResponse productSaveAndUpdate(Product product) {
        if (product != null){
            if (StringUtils.isNotBlank(product.getSubImages())){
                String []subImageArray = product.getSubImages().split(",");
                if (subImageArray.length>0){
                    product.setMainImage(subImageArray[0]);
                }
            }

            if (product.getId() != null){
                int rowCount = productMapper.updateByPrimaryKey(product);
                if (rowCount > 0){
                    return ServerResponse.createBySuccessMessage("产品更新成功");
                }else{
                    return ServerResponse.createByErrorMessage("产品更新失败");
                }
            }else{
                int rowCount = productMapper.insert(product);
                if (rowCount>0){
                    return ServerResponse.createByErrorMessage("产品添加成功");
                }else{
                    return ServerResponse.createByErrorMessage("产品添加失败");
                }
            }
        }
        return ServerResponse.createByErrorMessage("请输入正确的产品添加或者更新操作参数");

    }

    @Override
    public ServerResponse<String> setSaleStatus(Integer productId, Integer status) {
        if (productId == null || status == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Product product = new Product();
        product.setId(productId);
        product.setStatus(status);
        int rowCount = productMapper.updateByPrimaryKeySelective(product);
        if (rowCount>0){
            return ServerResponse.createBySuccessMessage("修改销售状态成功");
        }
        return ServerResponse.createByErrorMessage("修改销售状态失败");
    }

    @Override
    public ServerResponse<ProductDetailVo> manageProductDetail(Integer productId) {
        if (productId == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        Product product = productMapper.selectByPrimaryKey(productId);
        if (product == null){
            return ServerResponse.createByErrorMessage("产品已下架或者删除");
        }
        ProductDetailVo productDetailVo = assembleProductDetailVo(product);
        return ServerResponse.createBySuccessMessage(productDetailVo);

    }

    private ProductDetailVo assembleProductDetailVo(Product product){
        ProductDetailVo productDetailVo = new ProductDetailVo();
        productDetailVo.setId(product.getId());
        productDetailVo.setSubtitle(product.getSubtitle());
        productDetailVo.setCategoryId(product.getCategoryId());
        productDetailVo.setSubImages(product.getSubImages());
        productDetailVo.setMainImages(product.getMainImage());
        productDetailVo.setPrice(product.getPrice());
        productDetailVo.setDetail(product.getDetail());
        productDetailVo.setName(product.getName());
        productDetailVo.setStatus(product.getStatus());
        productDetailVo.setStock(product.getStock());

        productDetailVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix","http://img.happymmall.com/"));
        Category category = categoryMapper.selectByPrimaryKey(product.getCategoryId());
        if (category == null){
            productDetailVo.setParentCategoryId(0);
        }else{
            productDetailVo.setParentCategoryId(category.getParentId());
        }

        productDetailVo.setCreateTime(DateTimeUtil.dateToStr(product.getCreateTime()));
        productDetailVo.setUpdateTime(DateTimeUtil.dateToStr(product.getUpdateTime()));
        return productDetailVo;
    }

    @Override
    public ServerResponse<PageInfo> getList(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum,pageSize);

        List<ProductListVo> productListVoList = Lists.newArrayList();
        List<Product> productList = productMapper.selectList();
        for (Product product:productList){
            ProductListVo productListVo = assembleProductListVo(product);
            productListVoList.add(productListVo);
        }
        PageInfo pageResult = new PageInfo(productList);
        pageResult.setList(productListVoList);
        return ServerResponse.createBySuccessMessage(pageResult);

    }

    private ProductListVo assembleProductListVo(Product product){
        ProductListVo productListVo = new ProductListVo();
        productListVo.setId(product.getId());
        productListVo.setName(product.getName());
        productListVo.setCategoryId(product.getCategoryId());
        productListVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix","http://img.happymmall.com/"));
        productListVo.setMainImage(product.getMainImage());
        productListVo.setPrice(product.getPrice());
        productListVo.setSubtitle(product.getSubtitle());
        productListVo.setStatus(product.getStatus());
        return productListVo;
    }

    public ServerResponse<PageInfo> productSearch(String productName,Integer productId,int pageNum,int pageSize){
        PageHelper.startPage(pageNum,pageSize);
        if (StringUtils.isNotBlank(productName)){
            productName = new StringBuilder().append("%").append(productName).append("%").toString();
        }
        List<ProductListVo> productListVoList = Lists.newArrayList();
        List<Product> productList = productMapper.selectByProductNameAndId(productName,productId);
        for (Product product : productList){
            ProductListVo productListVo = assembleProductListVo(product);
            productListVoList.add(productListVo);
        }
        PageInfo pageResult = new PageInfo(productList);
        pageResult.setList(productListVoList);
        return ServerResponse.createBySuccessMessage(pageResult);
    }

    public ServerResponse<ProductDetailVo> getProductDetail(Integer productId){
        if (productId == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        Product product = productMapper.selectByPrimaryKey(productId);
        if (product == null){
            return ServerResponse.createByErrorMessage("产品已下架或者删除");
        }
        if(product.getStatus()!= Const.ProductStatusEnum.ON_SALE.getCode()){
            return ServerResponse.createByErrorMessage("商品已经下架");
        }
        ProductDetailVo productDetailVo = assembleProductDetailVo(product);
        return ServerResponse.createBySuccessMessage(productDetailVo);

    }

    public ServerResponse<PageInfo> getProductByKeywordCategory(String keyword,Integer categoryId,int pageNum,int pageSize,String orderBy){
        if (StringUtils.isBlank(keyword) &&categoryId == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        List<Integer> categoryIdList = Lists.newArrayList();
        if (categoryId != null){
            Category category = categoryMapper.selectByPrimaryKey(categoryId);
            if (category == null && StringUtils.isBlank(keyword)) {
                //没有该分类，并且还没有该关键字，返回空结果集，不报错
                PageHelper.startPage(pageNum, pageSize);
                List<ProductListVo> productListVoList = Lists.newArrayList();
                PageInfo pageInfo = new PageInfo(productListVoList);
                return ServerResponse.createBySuccessMessage(pageInfo);
            }
            categoryIdList = iCategoryService.getCategoryAndDeepChildrenCategory(category.getId()).getData();
        }
        if (StringUtils.isNotBlank(keyword)){
            keyword = new StringBuilder().append("%").append(keyword).append("%").toString();
        }
        PageHelper.startPage(pageNum,pageSize);
        //排序处理
        if (StringUtils.isNotBlank(orderBy)){
            if (Const.ProductListOrderBy.PRICE_ASC_DESC.contains(orderBy)){
                String []orderByArray = orderBy.split("_");
                PageHelper.orderBy(orderByArray[0]+" "+orderByArray[1]);
            }
        }
        List<Product> productList = productMapper.selectByProductNameAndCategoryIds(StringUtils.isBlank(keyword)?null:keyword,categoryIdList.size() == 0?null:categoryIdList);
        List<ProductListVo> productListVoList = Lists.newArrayList();
        for (Product product: productList) {
            ProductListVo productListVo = assembleProductListVo(product);
            productListVoList.add(productListVo);
        }
        PageInfo pageResult = new PageInfo(productList);
        pageResult.setList(productListVoList);
        return ServerResponse.createBySuccessMessage(pageResult);
    }
}
