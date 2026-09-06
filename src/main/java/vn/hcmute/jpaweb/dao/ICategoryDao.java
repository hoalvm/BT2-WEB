package vn.hcmute.jpaweb.dao;

import vn.hcmute.jpaweb.entity.Category;

import java.util.List;

public interface ICategoryDao {

    void insert(Category category);

    void update(Category category);

    void delete(int categoryId) throws Exception;

    Category findById(int categoryId);

    Category findByCategoryName(String name);

    List<Category> findAll();

    List<Category> searchByName(String keyword);

    List<Category> findAll(int page, int pageSize);

    List<Category> searchByName(String keyword, int page, int pageSize);

    int count();

    int countByName(String keyword);
}
