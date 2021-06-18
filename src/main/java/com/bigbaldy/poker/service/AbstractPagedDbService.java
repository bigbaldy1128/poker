package com.bigbaldy.poker.service;

import com.bigbaldy.poker.model.BaseModel;
import org.springframework.beans.factory.InitializingBean;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author wangjinzhao on 2020/5/15
 */
public abstract class AbstractPagedDbService<T extends BaseModel<ID>, ID> extends AbstractDbService<T, ID> implements InitializingBean {
    private List<AbstractIdSupplier<T, ID, ?, ?>> suppliers;

    @SuppressWarnings("unchecked")
    @Override
    public void afterPropertiesSet() throws IllegalAccessException {
        suppliers = new ArrayList<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.getType().isAssignableFrom(AbstractIdSupplier.class)) {
                field.setAccessible(true);
                suppliers.add((AbstractIdSupplier<T, ID, ?, ?>) field.get(this));
            }
        }
    }

    @Override
    public T save(T item) {
        for (AbstractIdSupplier<T, ID, ?, ?> supplier : suppliers) {
            supplier.save(item);
        }
        return super.save(item);
    }

    @Override
    public List<T> save(List<T> items) {
        for (AbstractIdSupplier<T, ID, ?, ?> supplier : suppliers) {
            supplier.save(items);
        }
        return super.save(items);
    }

    @Override
    public void remove(ID id) {
        Optional<T> itemOptional = super.get(id);
        if (!itemOptional.isPresent()) {
            return;
        }
        for (AbstractIdSupplier<T, ID, ?, ?> supplier : suppliers) {
            supplier.remove(itemOptional.get());
        }
        super.remove(id);
    }

    @Override
    public void remove(List<T> items) {
        for (AbstractIdSupplier<T, ID, ?, ?> supplier : suppliers) {
            supplier.remove(items);
        }
        super.remove(items);
    }


}
