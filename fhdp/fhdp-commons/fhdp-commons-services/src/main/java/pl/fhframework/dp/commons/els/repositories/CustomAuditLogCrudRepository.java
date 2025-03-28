package pl.fhframework.dp.commons.els.repositories;

public interface CustomAuditLogCrudRepository<T> {
    <S extends T> S save(S entity);

    <S extends T> Iterable<S> saveAll(Iterable<S> entities);
}
