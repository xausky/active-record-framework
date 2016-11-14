package io.github.xausky.arfe.interfaces;

/**
 * Created by xausky on 11/10/16.
 */
public interface UserInterface {
    default Integer getId() {
        return null;
    }

    default void setId(Integer id) {
    }

    default String getName() {
        return null;
    }

    default void setName(String name) {
    }
}
