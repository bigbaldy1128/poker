package com.bigbaldy.poker.service;

import com.bigbaldy.poker.model.BaseModel;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


public abstract class AbstractDbService<T extends BaseModel<ID>, ID> extends
        AbstractService<T, ID> {

    public abstract PagingAndSortingRepository<T, ID> getRepository();

    @Override
    public T saveToDb(T entity) {
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }

        entity.setUpdatedAt(Instant.now());
        return getRepository().save(entity);
    }

    @Override
    protected void deleteFromDbById(ID id) {
        getRepository().deleteById(id);
    }

    @Override
    protected void deleteFromDb(List<T> entity) {
        getRepository().deleteAll(entity);
    }

    @Override
    protected Iterable<T> saveAllToDb(List<T> list) {
        list.forEach(e -> {
            if (e.getCreatedAt() == null) {
                e.setCreatedAt(Instant.now());
            }
            e.setUpdatedAt(Instant.now());
        });
        return getRepository().saveAll(list);
    }

    @Override
    protected Iterable<T> findAllFromDbById(List<ID> ids) {
        return getRepository().findAllById(ids);
    }

    @Override
    protected Optional<T> findFromDbById(ID id) {
        return getRepository().findById(id);
    }
}
