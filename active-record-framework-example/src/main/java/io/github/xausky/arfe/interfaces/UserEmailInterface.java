package io.github.xausky.arfe.interfaces;

import io.github.xausky.arfe.User;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by xausky on 11/10/16.
 */
public interface UserEmailInterface {
    default String getEmail() {
        return null;
    }

    default void setEmail(String email) {
    }

    default List<User> selectByEmail(String email) throws SQLException {
        return null;
    }
}
