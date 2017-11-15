package com.mymmall.controller.backend;


import com.google.common.collect.Maps;
import com.mymmall.common.Const;
import com.mymmall.common.ResponseCode;
import com.mymmall.common.ServerResponse;
import com.mymmall.pojo.Product;
import com.mymmall.pojo.User;
import com.mymmall.service.IFileService;
import com.mymmall.service.IProductService;
import com.mymmall.service.IUserService;
import com.mymmall.util.PropertiesUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

@Controller
@RequestMapping(value = "/manage/product/")
public class ProductManageController {

    @Autowired
    private IProductService iProductService;
    @Autowired
    private IUserService iUserService;
    @Autowired
    private IFileService iFileService;

    /**
     * 保存或者更新产品信息
     * @param session
     * @param product
     * @return
     */

    @RequestMapping(value = "product_save.do")
    @ResponseBody
    public ServerResponse productSaveOrUpdate(HttpSession session, Product product){
        User currentUser = (User)session.getAttribute(Const.CURRENT_USER);
        if (currentUser == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"请使用管理员登陆");
        }
        if (iUserService.checkAdminRole(currentUser).isSuccess()){
            return iProductService.productSaveAndUpdate(product);
        }else{
            return ServerResponse.createByErrorMessage("您不是管理员，无操作权限！！！");
        }
    }

    @RequestMapping(value = "set_sale_status.do")
    @ResponseBody
    public ServerResponse<String> setSaleStatus(HttpSession session, Integer productId,Integer status){
        User currentUser = (User)session.getAttribute(Const.CURRENT_USER);
        if (currentUser == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"请使用管理员登陆");
        }
        if (iUserService.checkAdminRole(currentUser).isSuccess()){
            return iProductService.setSaleStatus(productId,status);
        }else{
            return ServerResponse.createByErrorMessage("您不是管理员，无操作权限！！！");
        }
    }

    /**
     * 获得商品的详细信息
     * @param session
     * @param productId
     * @return
     */
    @RequestMapping(value = "get_details.do")
    @ResponseBody
    public ServerResponse getDetails(HttpSession session, Integer productId){
        User currentUser = (User)session.getAttribute(Const.CURRENT_USER);
        if (currentUser == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"请使用管理员登陆");
        }
        if (iUserService.checkAdminRole(currentUser).isSuccess()){
            return iProductService.manageProductDetail(productId);
        }else{
            return ServerResponse.createByErrorMessage("您不是管理员，无操作权限！！！");
        }
    }

    /**
     * 分页查询功能的实现
     * @param session
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping(value = "list.do")
    @ResponseBody
    public ServerResponse getList(HttpSession session, @RequestParam(value = "pageNum",defaultValue = "1") Integer pageNum, @RequestParam(value = "pageSize",defaultValue = "10") Integer pageSize){
        User currentUser = (User)session.getAttribute(Const.CURRENT_USER);
        if (currentUser == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        if (iUserService.checkAdminRole(currentUser).isSuccess()){
            return iProductService.getList(pageNum,pageSize);
        }else{
            return ServerResponse.createByErrorMessage("您不是管理员，无操作权限！！！");
        }
    }

    /**
     * 商品搜索功能
     * @param session
     * @param productName
     * @param productId
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping(value = "search.do")
    @ResponseBody
    public ServerResponse productSearch(HttpSession session,String productName,Integer productId, @RequestParam(value = "pageNum",defaultValue = "1") Integer pageNum, @RequestParam(value = "pageSize",defaultValue = "10") Integer pageSize){
        User currentUser = (User)session.getAttribute(Const.CURRENT_USER);
        if (currentUser == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        if (iUserService.checkAdminRole(currentUser).isSuccess()){
            return iProductService.productSearch(productName,productId,pageNum,pageSize);
        }else{
            return ServerResponse.createByErrorMessage("您不是管理员，无操作权限！！！");
        }
    }

    /**
     * 上传文件功能
     * @param file
     * @param request
     * @return
     */
    @RequestMapping(value = "upload.do")
    @ResponseBody
    public ServerResponse upload(HttpSession session,@RequestParam(value ="upload_file",required = false)MultipartFile file, HttpServletRequest request){
        User currentUser = (User)session.getAttribute(Const.CURRENT_USER);
        if (currentUser == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        if (iUserService.checkAdminRole(currentUser).isSuccess()){
            String path = request.getSession().getServletContext().getRealPath("upload");
            String targetFileName = iFileService.upload(file,path);
            String url = PropertiesUtil.getProperty("ftp.server.http.prefix")+targetFileName;
            Map fileMap = Maps.newHashMap();
            fileMap.put("uri",targetFileName);
            fileMap.put("url",url);
            return ServerResponse.createBySuccessMessage(fileMap);
        }else{
            return ServerResponse.createByErrorMessage("您不是管理员，无操作权限");
        }
    }

    @RequestMapping(value = "richText_img_upload.do")
    @ResponseBody
    public Map richTextImgUpload(HttpSession session, @RequestParam(value ="upload_file",required = false)MultipartFile file, HttpServletRequest request, HttpServletResponse response){
        User currentUser = (User)session.getAttribute(Const.CURRENT_USER);
        Map resultMap = Maps.newHashMap();
        if (currentUser == null){
            resultMap.put("success",false);
            resultMap.put("msg","请登录管理员");
            return resultMap;
        }
//        富文本中对于返回值有自己的要求,我们使用是simditor所以按照simditor的要求进行返回
//        {
//            "success": true/false,
//                "msg": "error message", # optional
//            "file_path": "[real file path]"
//        }
        if (iUserService.checkAdminRole(currentUser).isSuccess()){
            String path = request.getSession().getServletContext().getRealPath("upload");
            String targetFileName = iFileService.upload(file,path);
            if (StringUtils.isBlank(targetFileName)){
                resultMap.put("success",false);
                resultMap.put("msg","上传失败");
                return resultMap;
            }
            String url = PropertiesUtil.getProperty("ftp.server.http.prefix")+targetFileName;
            resultMap.put("success",true);
            resultMap.put("msg","上传成功");
            resultMap.put("file_path",url);
            response.addHeader("Access-Control-Allow-Headers","X-File-Name");
            return resultMap;
        }else{
            resultMap.put("success",false);
            resultMap.put("msg","无权限操作");
            return resultMap;
        }
    }

}
