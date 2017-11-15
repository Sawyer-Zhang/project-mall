package com.mymmall.service;

import com.mymmall.common.ServerResponse;
import com.mymmall.pojo.User;

public interface IUserService {

    ServerResponse<User> login(String username,String password);

    ServerResponse<String> register(User user);

    ServerResponse<String> checkValid(String str,String type);

    ServerResponse<String> selectQuestion(String username);

    ServerResponse<String> forgetCheckAnswer(String username,String question,String answer);

    ServerResponse<String> forgetResetPassword(String username,String newPassword,String forgetToken);

    ServerResponse<String> resetPassword(String oldPassword,String newPassword,User user);

    ServerResponse<User> updateInformation(User user);

    ServerResponse<User> getInformation(Integer userId);

    ServerResponse<String> checkAdminRole(User user);
}
