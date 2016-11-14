package io.github.xausky.arfe.impls;

import io.github.xausky.arf.Model;
import io.github.xausky.arfe.User;
import io.github.xausky.arfe.interfaces.UserInterface;
import io.github.xausky.cfa.FusionImpl;

import javax.persistence.Column;
import javax.persistence.Id;

/**
 * Created by xausky on 11/10/16.
 */
@FusionImpl
public class UserImpl extends Model<User> implements UserInterface {
    @Id
    @Column
    private Integer id;

    @Column
    private String name;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
