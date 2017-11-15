package com.mymmall.service.impl;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mymmall.common.Const;
import com.mymmall.common.ResponseCode;
import com.mymmall.common.ServerResponse;
import com.mymmall.dao.CartMapper;
import com.mymmall.dao.ProductMapper;
import com.mymmall.pojo.Cart;
import com.mymmall.pojo.Product;
import com.mymmall.service.ICartService;
import com.mymmall.util.BigDecimalUtil;
import com.mymmall.util.PropertiesUtil;
import com.mymmall.vo.CartProductVo;
import com.mymmall.vo.CartVo;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service("iCartService")
public class CartServiceImpl implements ICartService{

    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;
    public ServerResponse<CartVo> getList(Integer userId){
        CartVo cartVo = this.getCartVoLimit(userId);
        return ServerResponse.createBySuccessMessage(cartVo);
    }
    public ServerResponse<CartVo> add(Integer userId,Integer productId,Integer count){
        if(productId == null || count == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        Cart cart = cartMapper.selectByUserIdProductId(userId,productId);
        //根据用户Id和产品Id搜索用户的购物车
        if (cart == null){
            //购物车为空
            //这个产品不在这个购物车中，需要新增一个这个产品的记录
            Cart cartItem = new Cart();
            cartItem.setQuantity(count);
            cartItem.setProductId(productId);
            cartItem.setUserId(userId);
            cartItem.setChecked(Const.Cart.CHECKED);
            cartMapper.insert(cartItem);
        }else{
            //这个产品已经在购物车中
            //在购物车中的产品直接数量相加
            count = cart.getQuantity() + count;
            cart.setQuantity(count);
            cartMapper.updateByPrimaryKeySelective(cart);
        }
        return this.getList(userId);
    }
    public ServerResponse<CartVo> update(Integer userId,Integer productId,Integer count){
        if(productId == null || count == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        Cart cart = cartMapper.selectByUserIdProductId(userId,productId);
        if (cart != null){
            cart.setQuantity(count);
        }
        cartMapper.updateByPrimaryKeySelective(cart);
        return this.getList(userId);
    }
    public ServerResponse<CartVo> deleteProduct(Integer userId,String productIds){
        List<String> productList = Splitter.on(",").splitToList(productIds);
        if (productList == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        cartMapper.deleteByUserIdAndProductIds(userId,productList);
        return this.getList(userId);
    }
    public ServerResponse<CartVo> selectOrUnSelect(Integer userId,Integer productId, Integer checked){
        cartMapper.checkedOrUncheckedProduct(userId,productId,checked);
        return this.getList(userId);
    }

    @Override
    public ServerResponse<Integer> getCartProductCount(Integer userId) {
        if (userId == null){
            return ServerResponse.createBySuccessMessage(0);
        }
        return ServerResponse.createBySuccessMessage(cartMapper.getCartProductCount(userId));

    }

    private CartVo getCartVoLimit(Integer userId){
        //使用BigDecimal的String构造器，可以防止出现精度问题
        CartVo cartVo = new CartVo();
        List<Cart> cartList = cartMapper.selectCartByUserId(userId);
        //根据用户Id查询用户的购物车
        List<CartProductVo> cartProductVoList = Lists.newArrayList();
        BigDecimal cartTotalPrice = new BigDecimal("0");
        if (CollectionUtils.isNotEmpty(cartList)){
            for (Cart cartItem:cartList){
                CartProductVo cartProductVo = new CartProductVo();
                cartProductVo.setId(cartItem.getId());
                cartProductVo.setUserId(cartItem.getUserId());
                cartProductVo.setProductId(cartItem.getProductId());
                //根据购物车中产品Id搜索产品，如果不为空说明存在此产品
                Product product = productMapper.selectByPrimaryKey(cartItem.getProductId());
                if (product != null){
                    cartProductVo.setProductName(product.getName());
                    cartProductVo.setProductMainImage(product.getMainImage());
                    cartProductVo.setProductSubtitle(product.getSubtitle());
                    cartProductVo.setProductPrice(product.getPrice());
                    cartProductVo.setProductStatus(product.getStatus());
                    cartProductVo.setProductStock(product.getStock());
                    //下面判断库存是否充足
                    int buyLimitCount = 0;
                    if (product.getStock()>=cartItem.getQuantity()){
                        //这个判断成立说明条件此时库存充足
                        buyLimitCount = cartItem.getQuantity();
                        cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_SUCCESS);
                    }else{
                        //此时，商品库存不足
                        buyLimitCount = product.getStock();
                        cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_FAIL);
                        Cart cartForQuantity = new Cart();
                        cartForQuantity.setQuantity(buyLimitCount);
                        cartForQuantity.setId(cartItem.getId());
                        cartMapper.updateByPrimaryKeySelective(cartForQuantity);
                    }
                    cartProductVo.setQuantity(buyLimitCount);
                    cartProductVo.setProductTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(),cartProductVo.getQuantity()));
                    cartProductVo.setProductChecked(cartItem.getChecked());
                }
                if (cartItem.getChecked() == Const.Cart.CHECKED){
                    cartTotalPrice = BigDecimalUtil.add(cartTotalPrice.doubleValue(),cartProductVo.getProductTotalPrice().doubleValue());
                }
                cartProductVoList.add(cartProductVo);
            }
        }
        cartVo.setCartTotalPrice(cartTotalPrice);
        cartVo.setCartProductVoList(cartProductVoList);
        cartVo.setAllChecked(this.getAllCheckedStatus(userId));
        cartVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));
        return cartVo;
    }
    private boolean getAllCheckedStatus(Integer userId){
        if (userId == null){
            return false;
        }
        //是否全选
        return cartMapper.selectCartProductCheckedStatusByUserId(userId) == 0;
    }

}
