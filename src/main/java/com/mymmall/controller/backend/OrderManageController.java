package com.mymmall.controller.backend;

import com.github.pagehelper.PageInfo;
import com.mymmall.common.Const;
import com.mymmall.common.ResponseCode;
import com.mymmall.common.ServerResponse;
import com.mymmall.pojo.User;
import com.mymmall.service.IOrderService;
import com.mymmall.service.IUserService;
import com.mymmall.vo.OrderVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/manage/order")
public class OrderManageController {

    @Autowired
    private IUserService iUserService;
    @Autowired
    private IOrderService iOrderService;

    @RequestMapping("/list.do")
    @ResponseBody
    public ServerResponse<PageInfo> getManageOrderList(HttpSession session,
                                                 @RequestParam(value = "pageNum",defaultValue = "1") int pageNum,
                                                 @RequestParam(value = "pageSize",defaultValue = "10") int pageSize){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if (user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，请管理员登录");
        }
        if (iUserService.checkAdminRole(user).isSuccess()){
            return iOrderService.getManageOrderList(pageNum,pageSize);
        }else{
            return ServerResponse.createByErrorMessage("无权限操作");
        }
    }
    @RequestMapping("/detail.do")
    @ResponseBody
    public ServerResponse<OrderVo> getManageOrderDetail(HttpSession session, Long orderNo){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if (user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，请管理员登录");
        }
        if (iUserService.checkAdminRole(user).isSuccess()){
            return iOrderService.getManageOrderDetail(orderNo);
        }else{
            return ServerResponse.createByErrorMessage("无权限操作");
        }
    }
    @RequestMapping("/search.do")
    @ResponseBody
    public ServerResponse<PageInfo> getManageOrderSearch(HttpSession session, Long orderNo,
                                                         @RequestParam(value = "pageNum",defaultValue = "1") int pageNum,
                                                         @RequestParam(value = "pageSize",defaultValue = "10") int pageSize){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if (user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，请管理员登录");
        }
        if (iUserService.checkAdminRole(user).isSuccess()){
            return iOrderService.getManageOrderSearch(orderNo,pageNum,pageSize);
        }else{
            return ServerResponse.createByErrorMessage("无权限操作");
        }
    }
    @RequestMapping("/send_goods.do")
    @ResponseBody
    public ServerResponse<String> manageSendGoods(HttpSession session, Long orderNo){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if (user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，请管理员登录");
        }
        if (iUserService.checkAdminRole(user).isSuccess()){
            return iOrderService.manageSendGoods(orderNo);
        }else{
            return ServerResponse.createByErrorMessage("无权限操作");
        }
    }

}
