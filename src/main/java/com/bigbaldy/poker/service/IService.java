package com.bigbaldy.poker.service;

import com.bigbaldy.poker.model.BaseModel;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IService<T extends BaseModel<ID>, ID> {

    Map<ID, T> getMap(@NotNull List<ID> ids);

    Map<ID, T> getMapFromDb(@NotNull List<ID> ids);

    Map<ID, T> getMapFromLocalCache(@NotNull List<ID> ids);

    Optional<T> get(@NotNull ID id);

    Optional<T> getFromDb(@NotNull ID id);

    Optional<T> getFromLocalCache(@NotNull ID id);

    List<T> get(@NotNull List<ID> ids);

    List<T> getFromDb(@NotNull List<ID> ids);

    T save(@NotNull T t);

    T saveToDb(@NotNull T t);

    List<T> save(@NotNull List<T> t);

    List<T> saveToDb(@NotNull List<T> t);

    void remove(@NotNull ID id);

    void removeFromDb(@NotNull ID id);

    void remove(@NotNull List<T> t);

    void removeFromDb(@NotNull List<T> t);

    List<T> getOnlyFromCache(@NotNull List<ID> ids);
}
