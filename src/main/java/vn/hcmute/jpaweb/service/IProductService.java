package vn.hcmute.jpaweb.service;

import vn.hcmute.jpaweb.entity.Product;

import java.util.List;

public interface IProductService {

    void insert(Product product);

    void update(Product product);

    void delete(int productId) throws Exception;

    Product findById(int productId);

    List<Product> findAll();

    Product findActiveById(int productId);

    List<Product> findActivePage(int page, int pageSize);

    default List<Product> findAllActive(int page, int pageSize) {
        return findActivePage(page, pageSize);
    }

    int countActive();

    List<Product> findNewestActive(int limit);
}
