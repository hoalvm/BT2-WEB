package vn.hcmute.jpaweb.dao;

import vn.hcmute.jpaweb.entity.User;

public interface IUserDao {

    void insert(User user);

    void update(User user);

    User findById(int userId);

    User findByUsername(String username);

    User findByEmail(String email);

    User findByUsernameOrEmail(String identifier);
}
