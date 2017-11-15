package com.mymmall.service.impl;

import com.mymmall.common.Const;
import com.mymmall.common.ServerResponse;
import com.mymmall.common.TokenCache;
import com.mymmall.dao.UserMapper;
import com.mymmall.pojo.User;
import com.mymmall.service.IUserService;
import com.mymmall.util.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("iUserService")
public class UserServiceImpl implements IUserService {
    @Autowired
    private UserMapper userMapper;
    @Override
    public ServerResponse<User> login(String username, String password) {
        int resultCount = userMapper.checkUsername(username);
        if (resultCount == 0){
            return ServerResponse.createByErrorMessage("用户名不存在");
        }

        String md5password = MD5Util.MD5EncodeUtf8(password);

        User user = userMapper.selectLogin(username, md5password);
        if(user == null){
            return ServerResponse.createByErrorMessage("密码错误！！！");
        }

        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccessMessage("登陆成功",user);

    }

    /**
     * 注册时检查用户名是否存在，邮箱是否存在
     * @param user
     * @return
     */
    public ServerResponse<String> register(User user){
        /*int resultCount = userMapper.checkUsername(user.getUsername());
        if (resultCount > 0){
            return ServerResponse.createByErrorMessage("用户名已存在");
        }*/
        /**
         * 这个部分重用checkValid方法中的代码
         * 注册时分别检查用户名的邮箱是否重复
         */
        ServerResponse validResponse = this.checkValid(user.getUsername(),Const.USERNAME);
        if (!validResponse.isSuccess()){
            return validResponse;
        }
        /*resultCount = userMapper.checkEmail(user.getEmail());
        if(resultCount > 0){
            return ServerResponse.createByErrorMessage("邮箱已存在");
        }*/
        validResponse = this.checkValid(user.getEmail(),Const.EMAIL);
        if(!validResponse.isSuccess()){
            return validResponse;
        }
        user.setRole(Const.Role.ROLE_CUSTOMER);
        //对于用户输入的密码进行加密，使用md5
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));

        int resultCount = userMapper.insert(user);
        if(resultCount == 0){
            return ServerResponse.createByErrorMessage("注册失败");
        }
        return ServerResponse.createBySuccessMessage("注册成功");
    }

    /**
     * 校验前台输入的value值
     * @param str
     * @param type
     * @return
     */
    public ServerResponse<String> checkValid(String str,String type) {
        if (StringUtils.isNotBlank(type)) {
            if (Const.USERNAME.equals(type)) {
                int resultCount = userMapper.checkUsername(str);
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage("用户名已存在");
                }
            }
            if (Const.EMAIL.equals(type)) {
                int resultCount = userMapper.checkEmail(str);
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage("email已存在");
                }
            }
        } else {
            return ServerResponse.createByErrorMessage("参数错误");
        }
        return ServerResponse.createBySuccessMessage("校验成功");
    }

    /**
     * 得到重置密码的问题
     */
    @Override
    public ServerResponse<String> selectQuestion(String username) {
        ServerResponse<String> validResponse = this.checkValid(username,Const.USERNAME);
        if (validResponse.isSuccess()){
            return ServerResponse.createByErrorMessage("用户名不存在");
        }
        String question = userMapper.selectQuestionByUsername(username);
        if (StringUtils.isNotBlank(question)){
            return ServerResponse.createBySuccessMessage(question);
        }
        return ServerResponse.createByErrorMessage("无法获取找到密码的问题");
    }

    /**
     * 检查重置密码的答案
     * @param username
     * @param question
     * @param answer
     * @return
     */
    @Override
    public ServerResponse<String> forgetCheckAnswer(String username, String question, String answer) {
        int resultCount = userMapper.checkAnswer(username,question,answer);
        if(resultCount>0){
            //当结果大于0时
            String forgetToken = UUID.randomUUID().toString();
            TokenCache.setKey(Const.TOKEN_PREFIX+username,forgetToken);
            return ServerResponse.createBySuccessMessage(forgetToken);
        }
        return ServerResponse.createByErrorMessage("回答的问题错误");

    }
    /**
     * 这是在还没登陆时进行密码的重置
     * 重置密码，查看token是否过期，比对token的值，将newPassword进行加密更新到数据库中，修改updateTime
     */
    @Override
    public ServerResponse<String> forgetResetPassword(String username, String newPassword, String forgetToken) {
        if (StringUtils.isBlank(forgetToken)){
            return ServerResponse.createByErrorMessage("请输入正确的");
        }
        ServerResponse validResponse = this.checkValid(username,Const.USERNAME);
        if(validResponse.isSuccess()){
            return ServerResponse.createByErrorMessage("用户不存在");
        }
        String token = TokenCache.getKey(Const.TOKEN_PREFIX + username);
        if (StringUtils.isBlank(token)){
            return ServerResponse.createByErrorMessage("token无效或者过期");
        }
        if (StringUtils.equals(token,forgetToken)){
            String md5Password = MD5Util.MD5EncodeUtf8(newPassword);
            int rowCount = userMapper.updatePasswordByUsername(username,md5Password);
                if (rowCount>0){
                return ServerResponse.createBySuccessMessage("密码重置成功");
            }
        }else{
            return ServerResponse.createByErrorMessage("token错误，请重新获取");
        }
        return ServerResponse.createByErrorMessage("重置密码失败，请再次尝试！！！");

    }

    /**
     * 在用户登录时进行密码修改，在更新密码时，必须指定用户id，这样可以避免横向越权，
     * @param oldPassword
     * @param newPassword
     * @param user
     * @return
     */
    @Override
    public ServerResponse<String> resetPassword(String oldPassword, String newPassword, User user) {
        int resultCount = userMapper.checkPassword(MD5Util.MD5EncodeUtf8(oldPassword),user.getId());
        if(resultCount==0){
            return ServerResponse.createByErrorMessage("请输入正确的旧密码");
        }
        user.setPassword(MD5Util.MD5EncodeUtf8(newPassword));
        int rowCount = userMapper.updateByPrimaryKeySelective(user);
        if(rowCount>0){
            return ServerResponse.createBySuccessMessage("密码修改成功");
        }
        return ServerResponse.createByErrorMessage("旧密码错误");
    }

    /**
     * 修改用户个人信息
     * 首先username不能被更改、
     * 其次要校验email，要校验除了当前用户以外，是否存在相同的email，如果校验时包含当前用户，会发生逻辑错误
     */

    @Override
    public ServerResponse<User> updateInformation(User user){
        int resultCount = userMapper.checkEmailByUserId(user.getEmail(),user.getId());
        if (resultCount > 0){
            return ServerResponse.createByErrorMessage("邮箱已存在，请重新输入邮箱");
        }
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setEmail(user.getEmail());
        updateUser.setPhone(user.getPhone());
        updateUser.setQuestion(user.getQuestion());
        updateUser.setAnswer(user.getAnswer());

        int updateCount = userMapper.updateByPrimaryKeySelective(updateUser);
        if(updateCount >0){
            return ServerResponse.createBySuccessMessage("更新用户信息成功",updateUser);
        }
        return ServerResponse.createByErrorMessage("更新用户信息失败");

    }

    @Override
    public ServerResponse<User> getInformation(Integer userId) {
        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null){
            return ServerResponse.createByErrorMessage("找不到该用户的ID");
        }
        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccessMessage(user);

    }

    @Override
    public ServerResponse<String> checkAdminRole(User user) {
        if (user.getRole().intValue() != 1){
            return ServerResponse.createByError();
        }
        return ServerResponse.createBySuccess();

    }
}
