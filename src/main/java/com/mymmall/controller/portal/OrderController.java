package com.mymmall.controller.portal;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.demo.trade.config.Configs;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.mymmall.common.Const;
import com.mymmall.common.ResponseCode;
import com.mymmall.common.ServerResponse;
import com.mymmall.pojo.User;
import com.mymmall.service.IOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Iterator;
import java.util.Map;

@Controller
@RequestMapping("/order/")
public class OrderController {

    private static Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private IOrderService iOrderService;

    @RequestMapping("pay.do")
    @ResponseBody
    public ServerResponse pay(HttpSession session, Long orderNo, HttpServletRequest request){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if (user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        String path = request.getSession().getServletContext().getRealPath("upload");
        return iOrderService.pay(orderNo,user.getId(),path);
    }
    @RequestMapping("alipay_callback.do")
    @ResponseBody
    public Object alipayCallback(HttpServletRequest request){
        Map<String,String> params = Maps.newHashMap();

        Map requestParameterMap = request.getParameterMap();
        Iterator iterator = requestParameterMap.keySet().iterator();
        while(iterator.hasNext()){
            String name = (String) iterator.next();
            String []values = (String [])requestParameterMap.get(name);
            String valStr = "";
            for (int i = 0;i<values.length;i++){
                valStr = (i==values.length-1)?values[i]:valStr+values[i]+",";
            }
            params.put(name,valStr);
        }
        logger.info("支付宝回调,sign:{},trade_status:{},参数:{}",params.get("sign"),params.get("trade_status"),params.toString());
        //检验回调的正确性，是不是支付宝发的，并且还要避免重复通知
        /**
         * 第一步： 在通知返回参数列表中，除去sign、sign_type两个参数外，凡是通知返回回来的参数皆是待验签的参数。
         * 第二步： 将剩下参数进行url_decode, 然后进行字典排序，组成字符串，得到待签名字符串
         * 第三步： 将签名参数（sign）使用base64解码为字节码串。
         * 第四步： 使用RSA的验签方法，通过签名字符串、签名参数（经过base64解码）及支付宝公钥验证签名。
         * 第五步：需要严格按照如下描述校验通知数据的正确性。
         */
        params.remove("sign_type");
        try {
            boolean alipayRSACheckedV2 = AlipaySignature.rsaCheckV2(params, Configs.getAlipayPublicKey(),"utf-8",Configs.getSignType());
            if (!alipayRSACheckedV2){
                return ServerResponse.createByErrorMessage("非法请求，验证不通过");
            }

        } catch (AlipayApiException e) {
            logger.error("支付宝验证回调异常");
        }


        // todo 这里还可以进行各种数据的验证

        ServerResponse serverResponse = iOrderService.alipayCallback(params);
        if (serverResponse.isSuccess()){
            return Const.AlipayCallback.RESPONSE_SUCCESS;
        }
        return Const.AlipayCallback.RESPONSE_FAILED;
    }
    @RequestMapping("query_order_pay_status.do")
    @ResponseBody
    public ServerResponse queryOrderPayStatus(HttpSession session, Long orderNo){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if (user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        ServerResponse serverResponse =  iOrderService.queryOrderPayStatus(orderNo,user.getId());
        if (serverResponse.isSuccess()){
            return ServerResponse.createBySuccessMessage(true);
        }
        return ServerResponse.createBySuccessMessage(false);
    }

    @RequestMapping("create")
    @ResponseBody
    public ServerResponse create(HttpSession session,Integer shippingId){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if (user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.createOrder(user.getId(),shippingId);
    }
    @RequestMapping("cancel")
    @ResponseBody
    public ServerResponse cancel(HttpSession session,Long orderNo){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if (user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.cancelOrder(user.getId(),orderNo);
    }

    /**
     * 获取购物车中被选中的商品信息，包括商品的主图，被选中商品的详细信息以及被选中商品的总价
     * @param session
     * @return
     */
    @RequestMapping("get_order_cart_product.do")
    @ResponseBody
    public ServerResponse getOrderCartProduct(HttpSession session){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if (user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.getOrderCartProduct(user.getId());
    }


    /**
     * 前台用户获取订单详情
     */
    @RequestMapping("get_order_detail.do")
    @ResponseBody
    public ServerResponse getOrderDetail(HttpSession session,Long orderNo){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if (user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.getOrderDetail(user.getId(),orderNo);
    }
    /**
     * 前台用户获取订单详情列表
     */
    @RequestMapping("get_order_list.do")
    @ResponseBody
    public ServerResponse<PageInfo> getOrderList(HttpSession session,
                                                 @RequestParam(value = "pageNum",defaultValue = "1") int pageNum,
                                                 @RequestParam(value = "pageSize",defaultValue = "10") int pageSize){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if (user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        return iOrderService.getOrderList(user.getId(),pageNum,pageSize);
    }
}
