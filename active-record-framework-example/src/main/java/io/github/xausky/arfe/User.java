package io.github.xausky.arfe;

import io.github.xausky.arf.Model;
import io.github.xausky.arfe.interfaces.UserEmailInterface;
import io.github.xausky.arfe.interfaces.UserInterface;
import io.github.xausky.cfa.Fusion;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.sql.DataSource;

/**
 * Created by xausky on 11/10/16.
 */
@Entity
@Fusion
public class User extends Model<User> implements UserInterface,UserEmailInterface{
}
