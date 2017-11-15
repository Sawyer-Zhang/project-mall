package com.mymmall.controller.portal;


import com.mymmall.common.Const;
import com.mymmall.common.ResponseCode;
import com.mymmall.common.ServerResponse;
import com.mymmall.pojo.User;
import com.mymmall.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/user/")
public class UserController {

    @Autowired
    private IUserService iUserService;

    /**
     * 用户登录的处理
     * @param username
     * @param password
     * @param session
     * @return
     * ServerResponse<T>类中有三个成员变量，其中data为泛型，我们通过createBySuccessMessage
     * 在登陆成功时将取出的User对象传递给data变量，通过getData()方法取出
     *
     */
    @RequestMapping(value="login.do",method= RequestMethod.POST)
    @ResponseBody  //将内容或者对象作为HTTP响应正文返回
    public ServerResponse<User> login(String username, String password, HttpSession session){
        ServerResponse<User> response =  iUserService.login(username,password);
        if(response.isSuccess()) {
            session.setAttribute(Const.CURRENT_USER, response.getData());
        }

        return response;
    }
    /**
     * 这里是用户注销的处理
     */
    @RequestMapping(value = "logout.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> logout(HttpSession session){
        session.removeAttribute(Const.CURRENT_USER);
        return ServerResponse.createBySuccess();

    }


    /**
     * 用户注册
     */
    @RequestMapping(value = "register.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> register(User user){
        return iUserService.register(user);
    }

    /**
     *  在用户在input中输入值时实时检验
     *  @param str value
     *  @param type 类型，是用户名还是邮箱
     */
    @RequestMapping(value = "check_valid.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> checkValid(String str,String type){
        return iUserService.checkValid(str,type);
    }

    /**
     * 从session中获取对象的信息
     * @param session
     * @return
     */
    @RequestMapping(value = "get_user_info.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> getUserInfo(HttpSession session){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        return ServerResponse.createBySuccessMessage(user);
    }
    @RequestMapping(value = "forget_user_question.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetGetQuestion(String username){
        return iUserService.selectQuestion(username);
    }

    /**
     * 根据用户名检查忘记密码的答案,使用guava本地缓存
     * @param username
     * @param question
     * @param answer
     * @return
     */
    @RequestMapping(value = "forget_check_answer.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetCheckAnswer(String username,String question,String answer){
        return iUserService.forgetCheckAnswer(username,question,answer);
    }

    /**
     * 这个是在没有登陆的情况下重置密码，即忘记密码
     * 重置密码,首先是调用忘记密码选项生成forgetToken，之后作为参数传入此方法中，通过key值取出token和参数比较
     * 因为可能会存在token，所以需要比较，如果过期了取出的是null，如果成功了，则更新数据库中对应的密码（先用md5进行加密）
     * @param username
     * @param newPassword
     * @param forgetToken
     * @return
     */
    @RequestMapping(value = "forget_reset_password.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetResetPassword(String username,String newPassword,String forgetToken){
        return iUserService.forgetResetPassword(username,newPassword,forgetToken);
    }

    /**
     * 在登录状态下进行密码重置，必须指定userID
     */
    @RequestMapping(value = "reset_password.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> resetPassword(HttpSession session,String oldPassword,String newPassword){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorMessage("用户未登录");
        }
        return iUserService.resetPassword(oldPassword,newPassword,user);

    }

    /**
     * 更新用户信息
     */
    @RequestMapping(value = "update_information.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> updateInformation(HttpSession session,User user){
        User currentUser = (User)session.getAttribute(Const.CURRENT_USER);
        if(currentUser == null){
            return ServerResponse.createByErrorMessage("请先登录");
        }
        user.setId(currentUser.getId());
        ServerResponse<User> response = iUserService.updateInformation(user);
        if (response.isSuccess()){
            response.getData().setUsername(currentUser.getUsername());
            session.setAttribute(Const.CURRENT_USER,response.getData());
        }
        return response;
    }


    /**
     * 得到用户的详细信息
     */
    @RequestMapping(value = "get_information.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> getInformation(HttpSession session){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if (user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户还没登陆，需要强制登录state=10");
        }
        return iUserService.getInformation(user.getId());

    }
}
