package com.mymmall.service;

import com.mymmall.common.ServerResponse;
import com.mymmall.pojo.Category;

import java.util.List;

public interface ICategoryService {

    ServerResponse<String> addCategory(String categoryName ,Integer parentId);
    ServerResponse<String> setCategoryName(Integer categoryId , String categoryName);
    ServerResponse<List<Category>> getChildParallelCategory(Integer categoryId);
    ServerResponse<List<Integer>> getCategoryAndDeepChildrenCategory(Integer categoryId);

}
