package com.mymmall.service.impl;

import com.alipay.api.AlipayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import com.alipay.demo.trade.utils.ZxingUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mymmall.common.Const;
import com.mymmall.common.ResponseCode;
import com.mymmall.common.ServerResponse;
import com.mymmall.dao.*;
import com.mymmall.pojo.*;
import com.mymmall.service.IOrderService;
import com.mymmall.util.BigDecimalUtil;
import com.mymmall.util.DateTimeUtil;
import com.mymmall.util.FTPUtil;
import com.mymmall.util.PropertiesUtil;
import com.mymmall.vo.OrderItemVo;
import com.mymmall.vo.OrderProductVo;
import com.mymmall.vo.OrderVo;
import com.mymmall.vo.ShippingVo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Service("iOrderService")
public class OrderServiceImpl implements IOrderService{

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private PayInfoMapper payInfoMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ShippingMapper shippingMapper;

    private static Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);


    public ServerResponse createOrder(Integer userId,Integer shippingId){
        //首先从购物车中获取选中的商品
        List<Cart> cartList = cartMapper.selectCheckedCartByUserId(userId);
        //计算这个订单的总价
        ServerResponse serverResponse = this.getCartOrderItem(userId,cartList);
        if (!serverResponse.isSuccess()){
            return serverResponse;
        }
        List<OrderItem> orderItemList = (List<OrderItem>) serverResponse.getData();
        BigDecimal payment = this.getOrderTotalPrice(orderItemList);
        //生成订单
        Order order = this.assembleOrder(userId,shippingId,payment);
        if (order == null){
            return ServerResponse.createByErrorMessage("生成订单错误");
        }
        if (CollectionUtils.isEmpty(orderItemList)){
            return ServerResponse.createByErrorMessage("购物车为空");
        }
        for (OrderItem orderItem:orderItemList){
            orderItem.setOrderNo(order.getOrderNo());
        }
        //这里是批量插入
        orderItemMapper.batchInsert(orderItemList);
        //生成订单成功之后，这时我们将库存减少
        this.reduceProductStock(orderItemList);
        //清空购物车
        this.cleanCart(cartList);


        OrderVo orderVo = assembleOrderVo(order,orderItemList);
        return ServerResponse.createBySuccessMessage(orderVo);
    }
    private OrderVo assembleOrderVo(Order order,List<OrderItem> orderItemList){
        OrderVo orderVo = new OrderVo();
        orderVo.setOrderNo(order.getOrderNo());
        orderVo.setPayment(order.getPayment());
        orderVo.setPaymentType(order.getPaymentType());
        orderVo.setPaymentTypeDesc(Const.PaymentTypeEnum.getPaymentTypeDesc(order.getPaymentType()).getStatus());

        orderVo.setPostage(order.getPostage());
        orderVo.setStatus(order.getStatus());
        orderVo.setStatusDesc(Const.OrderStatusEnum.getOrderStatusDesc(order.getStatus()).getStatus());
        orderVo.setShippingId(order.getShippingId());
        Shipping shipping = shippingMapper.selectByPrimaryKey(order.getShippingId());
        if (shipping != null){
            orderVo.setReceiverName(shipping.getReceiverName());
            orderVo.setShippingVo(assembleShippingVo(shipping));
        }
        orderVo.setPaymentTime(DateTimeUtil.dateToStr(order.getPaymentTime()));
        orderVo.setSendTime(DateTimeUtil.dateToStr(order.getSendTime()));
        orderVo.setCloseTime(DateTimeUtil.dateToStr(order.getCloseTime()));
        orderVo.setCreateTime(DateTimeUtil.dateToStr(order.getCreateTime()));
        orderVo.setEndTime(DateTimeUtil.dateToStr(order.getEndTime()));

        orderVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));
        List<OrderItemVo> orderItemVoList = Lists.newArrayList();
        for(OrderItem orderItem:orderItemList){
            OrderItemVo orderItemVo = assembleOrderItemVo(orderItem);
            orderItemVoList.add(orderItemVo);
        }
        orderVo.setOrderItemVoList(orderItemVoList);
        return orderVo;
    }
    private OrderItemVo assembleOrderItemVo(OrderItem orderItem){
        OrderItemVo orderItemVo = new OrderItemVo();
        orderItemVo.setOrderNo(orderItem.getOrderNo());
        orderItemVo.setProductId(orderItem.getProductId());
        orderItemVo.setProductName(orderItem.getProductName());
        orderItemVo.setProductImage(orderItem.getProductImage());
        orderItemVo.setCurrentUnitPrice(orderItem.getCurrentUnitPrice());
        orderItemVo.setQuantity(orderItem.getQuantity());
        orderItemVo.setTotalPrice(orderItem.getTotalPrice());
        return orderItemVo;
    }
    private ShippingVo assembleShippingVo(Shipping shipping){
        ShippingVo shippingVo = new ShippingVo();
        shippingVo.setReceiverName(shipping.getReceiverName());
        shippingVo.setReceiverAddress(shipping.getReceiverAddress());
        shippingVo.setReceiverCity(shipping.getReceiverCity());
        shippingVo.setReceiverDistrict(shipping.getReceiverDistrict());
        shippingVo.setReceiverProvince(shipping.getReceiverProvince());
        shippingVo.setReceiverPhone(shipping.getReceiverPhone());
        shippingVo.setReceiverMobile(shipping.getReceiverMobile());
        shippingVo.setReceiverZip(shipping.getReceiverZip());
        return shippingVo;
    }

    private void cleanCart(List<Cart> cartList){
        for (Cart cart:cartList){
            cartMapper.deleteByPrimaryKey(cart.getId());
        }
    }

    /**
     * 减少库存私有方法
     * 将订单项集合作为参数传入
     * 根据productId查出对应商品，将库存减去订单项中的数量
     * @param orderItemList
     */
    private void reduceProductStock(List<OrderItem> orderItemList){
        for (OrderItem orderItem:orderItemList){
            Product product = productMapper.selectByPrimaryKey(orderItem.getProductId());
            product.setStock(product.getStock()-orderItem.getQuantity());
            productMapper.updateByPrimaryKeySelective(product);
        }
    }

    private Order assembleOrder(Integer userId,Integer shippingId,BigDecimal payment){
        Order order = new Order();
        Long orderNo = this.generateOrderNo();
        order.setOrderNo(orderNo);
        order.setStatus(Const.OrderStatusEnum.NO_PAY.getCode());
        order.setPostage(0);
        order.setPaymentType(Const.PaymentTypeEnum.ONLINE_PAY.getCode());
        order.setPayment(payment);
        order.setUserId(userId);
        order.setShippingId(shippingId);

        int rowCount = orderMapper.insert(order);
        if (rowCount > 0){
            return order;
        }
        return null;
    }
    private Long generateOrderNo(){
        Long currentTime = System.currentTimeMillis();
        Long orderNo = currentTime + new Random().nextInt(100);
        return orderNo;
    }

    private BigDecimal getOrderTotalPrice(List<OrderItem> orderItemList){
        BigDecimal payment = new BigDecimal("0");
        for (OrderItem orderItem:orderItemList){
            payment = BigDecimalUtil.add(payment.doubleValue(),orderItem.getTotalPrice().doubleValue());
        }
        return payment;
    }

    /**
     * 将用户Id和购物车中被选中的商品封装成list作为参数传入
     * 将购物车中每一项被选中的商品取出，去检验这项产品的状态和数量检验库存，如果检验过后没有问题，将
     * 这项商品中的数据封装为一个orderItem对象添加到list集合中
     * @param userId
     * @param cartList
     * @return
     */
    private ServerResponse<List<OrderItem>> getCartOrderItem(Integer userId,List<Cart> cartList){
        List<OrderItem> orderItemList = Lists.newArrayList();
        if (CollectionUtils.isEmpty(cartList)){
            return ServerResponse.createByErrorMessage("购物车为空");
        }
        //校验购物车的数据，包括产品的状态和数量
        for (Cart cartItem: cartList){
            OrderItem orderItem = new OrderItem();
            Product product = productMapper.selectByPrimaryKey(cartItem.getProductId());
            if (Const.ProductStatusEnum.ON_SALE.getCode() != product.getStatus()) {
                return ServerResponse.createByErrorMessage("this product " + product.getName() + "is not on sale");
            }
            //检验库存
            if (cartItem.getQuantity() >= product.getStock()){
                return ServerResponse.createByErrorMessage("this product" + product.getName() + "doesn't have enough stock");
            }
            orderItem.setUserId(userId);
            orderItem.setProductId(product.getId());
            orderItem.setCurrentUnitPrice(product.getPrice());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(),cartItem.getQuantity()));
            orderItemList.add(orderItem);
        }
        return ServerResponse.createBySuccessMessage(orderItemList);
    }

    public ServerResponse pay(Long orderNo,Integer userId,String path) {

        Map<String, String> resultMap = Maps.newHashMap();
        Order order = orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
        if (order == null) {
            return ServerResponse.createByErrorMessage("该用户没下该订单");
        }
        resultMap.put("orderNo", String.valueOf(order.getOrderNo()));


        // (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        // 需保证商户系统端不能重复，建议通过数据库sequence生成，
        String outTradeNo = order.getOrderNo().toString();
                /*"tradeprecreate" + System.currentTimeMillis()
                + (long) (Math.random() * 10000000L);
*/
        // (必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店当面付扫码消费”
        String subject = new StringBuilder().append("少洋门店扫码支付，订单号：").append(outTradeNo).toString();

        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        String totalAmount = order.getPayment().toString();

        // (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
        // 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
        String undiscountableAmount = "0";

        // 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
        // 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
        String sellerId = "";

        // 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
        String body = new StringBuilder().append("订单").append(outTradeNo).append("购买商品共").append(totalAmount).append("元").toString();

        // 商户操作员编号，添加此参数可以为商户操作员做销售统计
        String operatorId = "test_operator_id";

        // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
        String storeId = "test_store_id";

        // 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
        ExtendParams extendParams = new ExtendParams();
        extendParams.setSysServiceProviderId("2088100200300400500");


        // 支付超时，定义为120分钟
        String timeoutExpress = "120m";

        // 商品明细列表，需填写购买商品详细信息，
        List<GoodsDetail> goodsDetailList = new ArrayList<GoodsDetail>();
        // 创建一个商品信息，参数含义分别为商品id（使用国标）、名称、单价（单位为分）、数量，如果需要添加商品类别，详见GoodsDetail

        List<OrderItem> orderItemList = orderItemMapper.selectByOrderNoAndUserId(orderNo, userId);
        for (OrderItem orderItem : orderItemList) {
            GoodsDetail goods1 = GoodsDetail.newInstance(orderItem.getProductId().toString(), orderItem.getProductName().toString()
                    , BigDecimalUtil.mul(orderItem.getCurrentUnitPrice().doubleValue(), new Double(100).doubleValue()).longValue()
                    , orderItem.getQuantity());
            // 创建好一个商品后添加至商品明细列表
            goodsDetailList.add(goods1);
        }


        // 继续创建并添加第一条商品信息，用户购买的产品为“黑人牙刷”，单价为5.00元，购买了两件
        /*GoodsDetail goods2 = GoodsDetail.newInstance("goods_id002", "xxx牙刷", 500, 2);
        goodsDetailList.add(goods2);
*/
        // 创建扫码支付请求builder，设置请求参数
        AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder()
                .setSubject(subject).setTotalAmount(totalAmount).setOutTradeNo(outTradeNo)
                .setUndiscountableAmount(undiscountableAmount).setSellerId(sellerId).setBody(body)
                .setOperatorId(operatorId).setStoreId(storeId).setExtendParams(extendParams)
                .setTimeoutExpress(timeoutExpress)
                .setNotifyUrl(PropertiesUtil.getProperty("alipay.callback.url"))//支付宝服务器主动通知商户服务器里指定的页面http路径,根据需要设置
                .setGoodsDetailList(goodsDetailList);

        /** 一定要在创建AlipayTradeService之前调用Configs.init()设置默认参数
         *  Configs会读取classpath下的zfbinfo.properties文件配置信息，如果找不到该文件则确认该文件是否在classpath目录
         */
        Configs.init("zfbinfo.properties");

        /** 使用Configs提供的默认参数
         *  AlipayTradeService可以使用单例或者为静态成员对象，不需要反复new
         */
        AlipayTradeService tradeService = new AlipayTradeServiceImpl.ClientBuilder().build();
        AlipayF2FPrecreateResult result = tradeService.tradePrecreate(builder);
        switch (result.getTradeStatus()) {
            case SUCCESS:
                logger.info("支付宝预下单成功: )");

                AlipayTradePrecreateResponse response = result.getResponse();
                dumpResponse(response);

                File file = new File(path);
                if (!file.exists()) {
                    file.setWritable(true);
                    file.mkdirs();
                }

                // 需要修改为运行机器上的路径
                // 二维码路径

                String qrPath = String.format(path + "/qr-%s.png", response.getOutTradeNo());
                String qrFileName = String.format("/qr-%s.png", response.getOutTradeNo());
                ZxingUtils.getQRCodeImge(response.getQrCode(), 256, qrPath);

                File targetFile = new File(path, qrFileName);
                try {
                    FTPUtil.uploadFile(Lists.newArrayList(targetFile));
                } catch (IOException e) {
                    logger.info("上传二维码异常", e);
                }

                logger.info("qrPath:" + qrPath);
                String qrUrl = PropertiesUtil.getProperty("ftp.server.http.prefix") + targetFile.getName();
                resultMap.put("qrUrl", qrUrl);
                return ServerResponse.createBySuccessMessage(resultMap);
            case FAILED:
                logger.error("支付宝预下单失败!!!");
                return ServerResponse.createByErrorMessage("支付宝预下单失败!!!");
            case UNKNOWN:
                logger.error("系统异常，预下单状态未知!!!");
                return ServerResponse.createByErrorMessage("系统异常，预下单状态未知!!!");
            default:
                logger.error("不支持的交易状态，交易返回异常!!!");
                return ServerResponse.createByErrorMessage("不支持的交易状态，交易返回异常!!!");
        }
    }
        // 简单打印应答
    private void dumpResponse(AlipayResponse response) {
        if (response != null) {
            logger.info(String.format("code:%s, msg:%s", response.getCode(), response.getMsg()));
            if (StringUtils.isNotEmpty(response.getSubCode())) {
                logger.info(String.format("subCode:%s, subMsg:%s", response.getSubCode(),
                        response.getSubMsg()));
            }
            logger.info("body:" + response.getBody());
        }
    }
    public ServerResponse alipayCallback(Map<String,String> params){
        Long orderNo = Long.parseLong(params.get("out_trade_no"));
        String tradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null){
            return ServerResponse.createByErrorMessage("错误订单，回调忽略");
        }
        if (order.getStatus()>= Const.OrderStatusEnum.PAID.getCode()){
            return ServerResponse.createBySuccessMessage("支付宝重复调用");
        }
        if (Const.AlipayCallback.TRADE_STATUS_TRADE_SUCCESS.equals(tradeStatus)){
            order.setPaymentTime(DateTimeUtil.strToDate(params.get("gmt_payment")));
            order.setStatus(Const.OrderStatusEnum.PAID.getCode());
            orderMapper.updateByPrimaryKeySelective(order);
        }
        PayInfo payInfo = new PayInfo();
        payInfo.setUserId(order.getUserId());
        payInfo.setOrderNo(order.getOrderNo());
        payInfo.setPayPlatform(Const.PayPlatformEnum.ALIPAY.getCode());
        payInfo.setPlatformNumber(tradeNo);
        payInfo.setPlatformStatus(tradeStatus);
        payInfoMapper.insert(payInfo);
        return ServerResponse.createBySuccess();
    }
    public ServerResponse queryOrderPayStatus(Long orderNo,Integer userId){
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null){
            return ServerResponse.createByErrorMessage("用户没有该订单");
        }
        if (order.getStatus()>= Const.OrderStatusEnum.PAID.getCode()){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }
    /**
     * 取消订单
     */
    public ServerResponse<String> cancelOrder(Integer userId,Long orderNo){
        Order order = orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
        if (order == null){
            return ServerResponse.createByErrorMessage("此用户订单不存在");
        }
        if (order.getStatus() != Const.OrderStatusEnum.NO_PAY.getCode()){
            return ServerResponse.createByErrorMessage("已付款，无法取消订单");
        }
        Order updateOrder = new Order();
        updateOrder.setUserId(userId);
        updateOrder.setOrderNo(orderNo);
        int row = orderMapper.updateByPrimaryKeySelective(updateOrder);
        if (row > 0){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }

    public ServerResponse getOrderCartProduct(Integer userId){
        OrderProductVo orderProductVo = new OrderProductVo();

        List<Cart> cartList = cartMapper.selectCheckedCartByUserId(userId);
        ServerResponse serverResponse = this.getCartOrderItem(userId,cartList);
        if (!serverResponse.isSuccess()){
            return serverResponse;
        }
        List<OrderItem> orderItemList = (List<OrderItem>)serverResponse.getData();
        List<OrderItemVo> orderItemVoList = Lists.newArrayList();
        BigDecimal payment = new BigDecimal("0");
        for (OrderItem orderItem:orderItemList){
            payment = BigDecimalUtil.add(payment.doubleValue(),orderItem.getTotalPrice().doubleValue());
            orderItemVoList.add(assembleOrderItemVo(orderItem));
        }
        orderProductVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));
        orderProductVo.setProductTotalPrice(payment);
        orderProductVo.setOrderItemVoList(orderItemVoList);
        return ServerResponse.createBySuccessMessage(orderProductVo);
    }

    public ServerResponse<OrderVo> getOrderDetail(Integer userId,Long orderNo){
        Order order = orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
        if (order == null){
            List<OrderItem> orderItemList = orderItemMapper.selectByOrderNoAndUserId(orderNo,userId);
            OrderVo orderVo = assembleOrderVo(order,orderItemList);
            return ServerResponse.createBySuccessMessage(orderVo);
        }
        return ServerResponse.createByErrorMessage("没有找到该订单");

    }
    public ServerResponse<PageInfo> getOrderList(Integer userId,int pageNum,int pageSize){
        PageHelper.startPage(pageNum, pageSize);
        List<Order> orderList = orderMapper.selectByUserId(userId);
        List<OrderVo> orderVoList = assembleOrderVoList(orderList,userId);
        PageInfo pageResult = new PageInfo(orderList);
        pageResult.setList(orderVoList);
        return ServerResponse.createBySuccessMessage(pageResult);
    }
    private List<OrderVo> assembleOrderVoList(List<Order> orderList,Integer userId){
        List<OrderVo> orderVoList = Lists.newArrayList();
        for (Order order:orderList){
            List<OrderItem> orderItemList = Lists.newArrayList();
            if (userId == null){
                //todo 管理员查询的时候，不需要传userId
                orderItemList = orderItemMapper.selectByOrderNo(order.getOrderNo());
            }else{
                orderItemList = orderItemMapper.selectByOrderNoAndUserId(order.getOrderNo(),userId);
            }
            OrderVo orderVo = assembleOrderVo(order,orderItemList);
            orderVoList.add(orderVo);
        }
        return orderVoList;
    }

    /**
     * 后台管理员获得订单列表
     */
    public ServerResponse<PageInfo> getManageOrderList(int pageNum,int pageSize){
        PageHelper.startPage(pageNum,pageSize);
        List<Order> orderList = orderMapper.selectAllOrder();
        List<OrderVo> orderVoList = this.assembleOrderVoList(orderList,null);
        PageInfo pageResult = new PageInfo(orderList);
        pageResult.setList(orderVoList);
        return ServerResponse.createBySuccessMessage(pageResult);
    }
    public ServerResponse<OrderVo> getManageOrderDetail(Long orderNo){
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order != null){
            List<OrderItem> orderItemList = orderItemMapper.selectByOrderNo(orderNo);
            OrderVo orderVo = this.assembleOrderVo(order,orderItemList);
            return ServerResponse.createBySuccessMessage(orderVo);
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }
    public ServerResponse<PageInfo> getManageOrderSearch(Long orderNo,int pageNum,int pageSize){
        PageHelper.startPage(pageNum, pageSize);
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order != null){
            List<OrderItem> orderItemList = orderItemMapper.selectByOrderNo(orderNo);
            OrderVo orderVo = this.assembleOrderVo(order,orderItemList);
            PageInfo pageResult = new PageInfo(Lists.newArrayList(order));
            pageResult.setList(Lists.newArrayList(orderVo));
            return ServerResponse.createBySuccessMessage(pageResult);
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }
    public ServerResponse<String> manageSendGoods(Long orderNo){
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order != null){
            if (order.getStatus() == Const.OrderStatusEnum.PAID.getCode()){
                order.setStatus(Const.OrderStatusEnum.SHIPPED.getCode());
                order.setSendTime(new Date());
                orderMapper.updateByPrimaryKeySelective(order);
                return ServerResponse.createBySuccessMessage("发货成功");
            }
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }
}