package pl.fhframework.dp.commons.services.auditlog;
import pl.fhframework.dp.transport.auditlog.AuditLogDto;

public interface IAuditLogDao {

    String persistDto(AuditLogDto auditLogDto);

    void indexData();

    AuditLogDto getDto(String key);

    void removeIndexedEntries();

    String getOpdata(String key, String opData);

    void markForIndexing();

    String getInstanceName();
}
