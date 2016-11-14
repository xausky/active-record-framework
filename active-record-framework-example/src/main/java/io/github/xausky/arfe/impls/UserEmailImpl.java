package io.github.xausky.arfe.impls;

import io.github.xausky.arf.Model;
import io.github.xausky.arfe.User;
import io.github.xausky.arfe.interfaces.UserEmailInterface;
import io.github.xausky.cfa.FusionImpl;

import javax.persistence.Column;
import java.sql.SQLException;
import java.util.List;
import java.util.StringJoiner;

/**
 * Created by xausky on 11/10/16.
 */
@FusionImpl
public class UserEmailImpl extends Model<User> implements UserEmailInterface {
    @Column
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public List<User> selectByEmail(String email) throws SQLException {
        return select("SELECT * FROM " + modelConfig.getTable() + " WHERE email=?", getEmail());
    }
}
