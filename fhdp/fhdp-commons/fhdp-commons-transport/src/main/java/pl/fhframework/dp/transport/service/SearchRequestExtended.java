package pl.fhframework.dp.transport.service;

import lombok.Data;
import pl.fhframework.dp.transport.dto.commons.BaseDtoQuery;

@Data
public class SearchRequestExtended <QUERY extends BaseDtoQuery> {
    QUERY query;
    Integer limit;
}
