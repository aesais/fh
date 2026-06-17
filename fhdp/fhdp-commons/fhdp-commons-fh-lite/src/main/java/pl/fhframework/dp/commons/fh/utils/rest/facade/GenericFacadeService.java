package pl.fhframework.dp.commons.fh.utils.rest.facade;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import pl.fhframework.dp.commons.base.model.IPersistentObject;
import pl.fhframework.dp.transport.dto.commons.BaseDtoQuery;
import pl.fhframework.dp.transport.dto.commons.NameValueDto;
import pl.fhframework.dp.transport.service.IDtoService;
import pl.fhframework.dp.transport.service.SearchRequestExtended;
import pl.fhframework.dp.transport.service.SearchResultExtended;
import pl.fhframework.model.forms.PageModel;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static pl.fhframework.dp.transport.service.TotalHitsRelationFH.GREATER_THAN_OR_EQUAL_TO;

/**
 * @author <a href="mailto:jacek.borowiec@asseco.pl">Jacek Borowiec</a>
 * @version :  $, :  $
 * @created 26.08.2019
 */
public class GenericFacadeService<ID, DTO extends IPersistentObject, LIST extends IPersistentObject, QUERY extends BaseDtoQuery> implements IDtoService<ID, DTO , LIST , QUERY> {
    @Autowired
    protected FacadeClientFactory clientFactory;
    private Class serviceClazz;

    public GenericFacadeService(Class serviceClazz) {
        this.serviceClazz = serviceClazz;
    }

    @Override
    public NameValueDto getCode(String refDataCode, String code, LocalDate onDate, Map params) {
        IDtoService service = clientFactory.createServiceProxy(serviceClazz);
        return service.getCode(refDataCode, code, onDate, params);
    }

    @Override
    public List<NameValueDto> listCodeList(String code, String text, LocalDate onDate, Map params) {
        IDtoService service = clientFactory.createServiceProxy(serviceClazz);
        return service.listCodeList(code, text, onDate, params);
    }

    public List<NameValueDto> listCodeListPageable(Pageable pageable, String code, String text, LocalDate onDate, Map params) {
        IDtoService service = clientFactory.createServiceProxy(serviceClazz);
        params.put("page", String.valueOf(pageable.getPageNumber()));
        params.put("size", String.valueOf(pageable.getPageSize()));
        return service.listCodeList(code, text, onDate, params);
    }

    public PageModel<NameValueDto> listCodeListPaged(String code, String text, LocalDate onDate, Map params) {
        long total = countCodeList(code, text, onDate, params);
        return new PageModel<NameValueDto>(pageable -> loadCodeListPage(pageable, code, text, onDate, params, total));

    }

    private Page<NameValueDto> loadCodeListPage(Pageable pageable, String code, String text, LocalDate onDate, Map params, long total) {
        IDtoService restService = clientFactory.createServiceProxy(serviceClazz);

        Page<NameValueDto> ret = new PageImpl<NameValueDto>(listCodeListPageable(pageable, code, text, onDate, params), pageable, total);
        return ret;
    }

    public Long countCodeList(String code, String text, LocalDate onDate, Map params) {
        IDtoService service = clientFactory.createServiceProxy(serviceClazz);
        return service.countCodeList(code, text, onDate, params);
    }

    @Override
    public List<LIST> listDto(QUERY query) {
        if(query == null) {
            throw new RuntimeException("query can not be null");
        }
        IDtoService restService = clientFactory.createServiceProxy(serviceClazz);
        return restService.listDto(query);
    }

    @Override
    public SearchResultExtended<LIST> listDtoExtended(SearchRequestExtended searchRequestExtended) {
        if(searchRequestExtended == null) {
            throw new RuntimeException("query can not be null");
        }
        IDtoService restService = clientFactory.createServiceProxy(serviceClazz);
        return restService.listDtoExtended(searchRequestExtended);
    }

    public PageModel<LIST> listDtoPaged(QUERY query) {
        return listDtoPagedExtended(query, null);
    }

    public PageModel<LIST> listDtoPagedExtended(QUERY query, Integer limit) {
        if(query == null) {
            throw new RuntimeException("query can not be null");
        }
        query.setFirstRow(0);
        query.setSize(10);
        return new PageModel<>(createDataSource(query, limit));
    }

    public Function<Pageable, Page<LIST>> createDataSource(QUERY query, Integer limit) {
        if(query == null) {
            throw new RuntimeException("query can not be null");
        }
        final long totalCount = limit == null ? listCount(query) : 0L ;
        query.setFirstRow(0);
        query.setSize(10);
        return  pageable -> limit == null ?
                loadRegisterHFPage(pageable, query, totalCount)
                :
                loadRegisterHFPageWithLimit(pageable, query, limit);
    }


    public List<LIST> listDtoPageable(Pageable pageable, QUERY query) {
        if(query == null) {
            throw new RuntimeException("query can not be null");
        }
        updateQuery(query, pageable);
        IDtoService restService = clientFactory.createServiceProxy(serviceClazz);
        return restService.listDto(query);
    }

    /**
     * Loads a paginated list of DTOs based on the given query and pageable parameters.
     * The method updates the query with pagination details, sends a request to the service to
     * fetch the data, and returns the result wrapped in a Page object with information
     * regarding the pagination and total hits.
     * <p>
     * This version does not require calculation of total elements - one call less.
     *
     * @param pageable provides pagination and sorting information, including page number,
     *                 page size, and sort order.
     * @param query the query object containing filters and criteria to fetch the desired data.
     * @return a Page object containing the fetched DTOs, pagination metadata, and whether the
     *         configured limit was reached.
     */
    public Page<LIST> loadRegisterHFPageWithLimit(Pageable pageable, QUERY query, Integer limit) {
        updateQuery(query, pageable);
        IDtoService restService = clientFactory.createServiceProxy(serviceClazz);
        SearchRequestExtended<QUERY> sre = new SearchRequestExtended<>();
        sre.setQuery(query);

        sre.setLimit(limit);

        SearchResultExtended res = restService.listDtoExtended(sre);
        return new PageImplWithLimit(res.getList(), pageable, res.getTotalHits(),
                                                     res.getHitsRelation() == GREATER_THAN_OR_EQUAL_TO);
    }

    public Page<LIST> loadRegisterHFPage(Pageable pageable, QUERY query, long total) {
        updateQuery(query, pageable);
        return new PageImpl<>(listDtoPageable(pageable, query), pageable, total);
    }

    private void updateQuery(QUERY query, Pageable pageable) {
        query.setFirstRow(Math.toIntExact(pageable.getOffset()));
        query.setSize(pageable.getPageSize());
        updateSortOrder(query, pageable.getSort());
    }

    public void updateSortOrder(BaseDtoQuery query, Sort sort) {
        String sortProperty = query.getSortProperty();
        Boolean ascending = query.getAscending();
        if (sort != null && sort.isSorted()) {
            Optional<Sort.Order> sOpt = sort.stream().findFirst();
            if (sOpt.isPresent()) {
                Sort.Order so = sOpt.get();
                sortProperty = so.getProperty();
                ascending = so.getDirection() != null && so.getDirection().isAscending();
            }
        }
        query.setAscending(ascending);
        query.setSortProperty(sortProperty);
    }

    @Override
    public Long listCount(QUERY query) {
         return clientFactory.createServiceProxy(serviceClazz).listCount(query);
    }

    @Override
    public DTO getDto(ID key) {
        return (DTO) clientFactory.createServiceProxy(serviceClazz).getDto(key);
    }

    @Override
    public ID persistDto(DTO dto) {
        return (ID) clientFactory.createServiceProxy(serviceClazz).persistDto(dto);
    }

    @Override
    public void deleteDto(ID key) {
        clientFactory.createServiceProxy(serviceClazz).deleteDto(key);
    }
}
