package com.mymmall.service.impl;


import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mymmall.common.ServerResponse;
import com.mymmall.dao.CategoryMapper;
import com.mymmall.pojo.Category;
import com.mymmall.service.ICategoryService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service(value = "iCategoryService")
public class CategoryServiceImpl implements ICategoryService{

    private Logger logger = LoggerFactory.getLogger(CategoryServiceImpl.class);
    @Autowired
    private CategoryMapper categoryMapper;

    public ServerResponse<String> addCategory(String categoryName ,Integer parentId){
        if (parentId == null || StringUtils.isBlank(categoryName)){
            return ServerResponse.createByErrorMessage("添加商品参数错误");
        }

        Category category = new Category();
        category.setName(categoryName);
        category.setParentId(parentId);
        category.setStatus(true);
        int rowCount = categoryMapper.insert(category);
        if(rowCount > 0){
            return ServerResponse.createBySuccessMessage("商品添加成功");
        }
        return ServerResponse.createByErrorMessage("添加商品失败");
    }

    public ServerResponse<String> setCategoryName(Integer categoryId , String categoryName){
        if (categoryId == null || StringUtils.isBlank(categoryName)){
            return ServerResponse.createByErrorMessage("设置品类名称参数错误");
        }
        Category category = new Category();
        category.setId(categoryId);
        category.setName(categoryName);
        int rowCount = categoryMapper.updateByPrimaryKeySelective(category);
        if (rowCount > 0){
            return ServerResponse.createBySuccessMessage("商品设置品类名称成功");
        }
        return ServerResponse.createByErrorMessage("商品设置品类名称失败");
    }

    @Override
    public ServerResponse<List<Category>> getChildParallelCategory(Integer categoryId) {
        if (categoryId == null){
            return ServerResponse.createByErrorMessage("传入参数错误");
        }
        List<Category> list = categoryMapper.selectCategoryChildrenByParentId(categoryId);
        if (CollectionUtils.isEmpty(list)){
            logger.error("该品类下没有子节点");
        }
        return ServerResponse.createBySuccessMessage(list);
    }

    @Override
    public ServerResponse<List<Integer>> getCategoryAndDeepChildrenCategory(Integer categoryId) {
        Set<Category> categorySet = Sets.newHashSet();
        findChildrenCategory(categorySet,categoryId);

        List<Integer> categoryList = Lists.newArrayList();
        if (categoryId != null) {
            for (Category categoryItem : categorySet) {
                categoryList.add(categoryItem.getId());
            }
        }
        return ServerResponse.createBySuccessMessage(categoryList);
    }

    /**
     * 递归函数找到所有的子节点存储到set集合中，可以去重，不过因为存储的不是基本类型，所以需要重写
     * equals和hashcode方法。
     * @param categoryId
     * @return
     */
    private Set<Category> findChildrenCategory(Set<Category> categorySet,Integer categoryId){
        Category category = categoryMapper.selectByPrimaryKey(categoryId);
        if (category!=null){
            categorySet.add(category);
        }
        List<Category> categoryList = categoryMapper.selectCategoryChildrenByParentId(categoryId);
        for (Category CategoryItem : categoryList){
            findChildrenCategory(categorySet,CategoryItem.getId());
        }
        return categorySet;
    }
}
