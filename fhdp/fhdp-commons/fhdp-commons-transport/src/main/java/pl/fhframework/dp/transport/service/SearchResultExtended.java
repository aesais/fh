package pl.fhframework.dp.transport.service;

import lombok.Data;
import pl.fhframework.dp.commons.base.model.IPersistentObject;

import java.util.List;

@Data
public class SearchResultExtended<LIST extends IPersistentObject> {
    List<LIST> list;
    TotalHitsRelationFH hitsRelation;
    Long totalHits;
}
