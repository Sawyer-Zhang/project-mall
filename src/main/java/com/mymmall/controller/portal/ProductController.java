package com.mymmall.controller.portal;


import com.github.pagehelper.PageInfo;
import com.mymmall.common.ServerResponse;
import com.mymmall.service.IProductService;
import com.mymmall.vo.ProductDetailVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
@RequestMapping("/product/")
public class ProductController {

    @Autowired
    private IProductService iProductService;

    /**
     * 前台获取产品详情，额外要加一个判断是否是ON_SALE
     * @param productId
     * @return
     */
    @RequestMapping("detail.do")
    @ResponseBody
    public ServerResponse<ProductDetailVo> detail(Integer productId){
        return iProductService.getProductDetail(productId);
    }

    /**
     * 此功能是实现用户端的产品搜索，主要实现思路是在分页功能的基础上进行判断，
     * 模糊查询关键词和对分类ID进行查询
     * @param keyword 关键词
     * @param categoryId 分类ID
     * @param pageNum  分页号
     * @param pageSize  每一页的记录数
     * @param orderBy   排序
     * @return
     */
    @RequestMapping("list.do")
    @ResponseBody
    public ServerResponse<PageInfo> list(@RequestParam(value = "keyword",required = false)String keyword,
                                         @RequestParam(value = "categoryId",required = false)Integer categoryId,
                                         @RequestParam(value = "pageNum",defaultValue = "1")int pageNum,
                                         @RequestParam(value = "pageSize",defaultValue = "10")int pageSize,
                                         @RequestParam(value = "orderBy",defaultValue = "")String orderBy){
        return iProductService.getProductByKeywordCategory(keyword, categoryId, pageNum, pageSize, orderBy);

    }
}
