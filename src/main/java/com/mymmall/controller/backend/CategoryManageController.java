package com.mymmall.controller.backend;


import com.mymmall.common.Const;
import com.mymmall.common.ResponseCode;
import com.mymmall.common.ServerResponse;
import com.mymmall.pojo.Category;
import com.mymmall.pojo.User;
import com.mymmall.service.ICategoryService;
import com.mymmall.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.*;

@Controller
@RequestMapping(value = "/manage/category/")
public class CategoryManageController {
    @Autowired
    private IUserService iUserService;
    @Autowired
    private ICategoryService iCategoryService;

    /**
     * 添加商品品类，只有管理员才有资格添加，所以首先要校验是否已经登陆，之后校验
     * 是否是管理员登陆，如果是管理员登陆，则进行添加操作，否则返回没有权限
     * @param session
     * @param categoryName
     * @param parentId
     * @return
     */
    @RequestMapping(value = "add_category.do")
    @ResponseBody
    public ServerResponse<String> addCategory(HttpSession session,String categoryName,@RequestParam(value = "parentId",defaultValue = "0") int parentId){
        User currentUser = (User) session.getAttribute(Const.CURRENT_USER);
        if (currentUser == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户还没有登录");
        }
        if (iUserService.checkAdminRole(currentUser).isSuccess()){
            return iCategoryService.addCategory(categoryName,parentId);
        }else{
            return ServerResponse.createByErrorMessage("对不起，您不是管理员，无法进行添加操作");
        }
    }

    /**
     * 更新商品品类名称
     * @return
     */
    @RequestMapping(value = "set_category_name.do")
    @ResponseBody
    public ServerResponse setCategoryName(HttpSession session,Integer categoryId,String categoryName){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if (user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户还没有登录");
        }
        if (iUserService.checkAdminRole(user).isSuccess()){
            return iCategoryService.setCategoryName(categoryId,categoryName);
        }else{
            return ServerResponse.createByErrorMessage("对不起，您不是管理员，无法进行设置品类名称操作");
        }
    }

    /**
     * 获得商品品类所有子节点的商品信息，不递归，查询平级的子节点
     */
    @RequestMapping(value = "get_category.do")
    @ResponseBody
    public ServerResponse<List<Category>> getChildParallelCategory(HttpSession session,@RequestParam(value = "categoryId",defaultValue = "0") Integer categoryId){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"请先登录");
        }
        if (iUserService.checkAdminRole(user).isSuccess()){
            return iCategoryService.getChildParallelCategory(categoryId);
        }else{
            return ServerResponse.createByErrorMessage("对不起，您不是管理员，无法进行设置品类名称操作");
        }
    }
    /**
     * 递归获得该节点的所有子节点，以及所有子节点的子节点
     */
    @RequestMapping(value = "get_deep_category.do")
    @ResponseBody
    public ServerResponse<List<Integer>> getCategoryAndDeepChildrenCategory(HttpSession session,@RequestParam(value = "categoryId",defaultValue = "0") Integer categoryId){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"请先登录");
        }
        if (iUserService.checkAdminRole(user).isSuccess()){
            return iCategoryService.getCategoryAndDeepChildrenCategory(categoryId);
        }else{
            return ServerResponse.createByErrorMessage("对不起，您不是管理员，不能进行相关操作");
        }
    }
}
