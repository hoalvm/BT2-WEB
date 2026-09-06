package vn.hcmute.jpaweb.dao;

import vn.hcmute.jpaweb.entity.Product;

import java.util.List;

public interface IProductDao {

    void insert(Product product);

    void update(Product product);

    void delete(int productId) throws Exception;

    Product findById(int productId);

    List<Product> findAll();

    Product findActiveById(int productId);

    List<Product> findActivePage(int page, int pageSize);

    int countActive();

    List<Product> findNewestActive(int limit);
}
