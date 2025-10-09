package pl.fhframework.dp.commons.services.facade;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.NestedSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import pl.fhframework.dp.commons.base.model.IPersistentObject;
import pl.fhframework.dp.commons.els.config.ElasticSearchParams;
import pl.fhframework.dp.commons.utils.conversion.BeanConversionUtil;
import pl.fhframework.dp.transport.dto.commons.BaseDtoQuery;
import pl.fhframework.dp.transport.service.IDtoService;
import pl.fhframework.dp.transport.service.SearchRequestExtended;
import pl.fhframework.dp.transport.service.SearchResultExtended;
import pl.fhframework.dp.transport.service.TotalHitsRelationFH;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * @author <a href="mailto:jacek.borowiec@asseco.pl">Jacek Borowiec</a>
 * @version :  $, :  $
 * @created 25.08.2019
 */
@Service
@Slf4j
public abstract class GenericDtoService<ID,
        DTO extends IPersistentObject,
        LIST extends IPersistentObject,
        QUERY extends BaseDtoQuery,
        ENTITY> implements IDtoService<ID, DTO , LIST , QUERY> {

    @Autowired
    ElasticSearchParams elasticSearchParams;
    @Autowired
    protected ElasticsearchOperations elasticsearchTemplate;
    private Class<LIST> listClazz;
    private Class<DTO> dtoClazz;
    private Class<ENTITY> entityClazz;


    public GenericDtoService(Class<LIST> listClass, Class<DTO> dtoClazz, Class<ENTITY> entityClazz) {
        this.listClazz = listClass;
        this.dtoClazz = dtoClazz;
        this.entityClazz = entityClazz;
    }

    @Override
    public SearchResultExtended<LIST> listDtoExtended(SearchRequestExtended searchRequestExtended) {
        QUERY query = (QUERY)searchRequestExtended.getQuery();
        Integer trackTotalHitsUpTo = searchRequestExtended.getLimit();
        BoolQueryBuilder queryBuilder = createQueryBuilderInternal(query);
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder()
              .withQuery(queryBuilder);
        if(query.getFirstRow() != null && query.getSize() != null) {
            SortBuilder<?> sortWithNested = new FieldSortBuilder("id")
                  .order(SortOrder.ASC)
                  .setNestedSort(null);

            Pageable pageable;

            pageable = PageRequest.of(query.getFirstRow()/query.getSize(), query.getSize());

            // bez sortowania jesli SortProperty = "0"
            if(query.getSortProperty() != null) {
                if (!query.getSortProperty().equals("0")) {
                    String nestedPath = getNestedPath(dtoClazz, query.getSortProperty());
                    sortWithNested = new FieldSortBuilder(query.getSortProperty())
                          .order((query.getAscending() != null && !query.getAscending()) ? SortOrder.DESC : SortOrder.ASC)
                          .setNestedSort(nestedPath != null ? new NestedSortBuilder(nestedPath) : null);
                }
            }


            searchQueryBuilder = searchQueryBuilder.withPageable(pageable).withSorts(sortWithNested);
        }
        NativeSearchQuery searchQuery = searchQueryBuilder.build();
        if (trackTotalHitsUpTo !=null) {
            searchQuery.setTrackTotalHitsUpTo(trackTotalHitsUpTo);
        }

        SearchResultExtended<LIST> ret = new SearchResultExtended<>();
        try {
            IndexCoordinates indexCoordinates = elasticsearchTemplate.getIndexCoordinatesFor(listClazz);

            SearchHits<LIST> res = elasticsearchTemplate.search(searchQuery, listClazz, indexCoordinates);
            List<LIST> list = res.getSearchHits().stream().map(SearchHit::getContent).collect(toList());
            ret.setList(list);
            ret.setTotalHits(res.getTotalHits());
            ret.setHitsRelation(mapTotalHitsRelation2FH(res.getTotalHitsRelation()));
        } catch (Exception e) {
            // for compatibility - in case of error empty list is returned
            ret.setList(new ArrayList<>());
            log.warn("{}", ExceptionUtils.getStackTrace(e));
        }
        return ret;
    }

    public static TotalHitsRelationFH mapTotalHitsRelation2FH(TotalHitsRelation totalHitsRelation) {
        if (totalHitsRelation == null) {
            return null;
        }
        switch (totalHitsRelation) {
            case EQUAL_TO:
                return TotalHitsRelationFH.EQUAL_TO;
            case GREATER_THAN_OR_EQUAL_TO:
                return TotalHitsRelationFH.GREATER_THAN_OR_EQUAL_TO;
            default:
                return TotalHitsRelationFH.OFF;
        }
    }

    @Override
    public List<LIST> listDto(QUERY query) {
        SearchRequestExtended searchRequestExtended = new SearchRequestExtended();
        searchRequestExtended.setQuery(query);
        SearchResultExtended<LIST> res = listDtoExtended(searchRequestExtended);
        return res.getList();
    }

    protected BoolQueryBuilder createQueryBuilderInternal(QUERY query) {
        BoolQueryBuilder ret = QueryBuilders.boolQuery();
        if(query.getTextSearch() != null) {
            String txt = escapeSpecialCharacters(query.getTextSearch() +
                            (query.isWholeWordsOnly()? "": "*"));
            ret.must(QueryBuilders.queryStringQuery(txt)
                    .analyzeWildcard(true)
                    .allowLeadingWildcard(false));
        }
        ret = extendQueryBuilder(ret, query);
        return ret;
    }
    //TODO: English
    /**
     *
     * Metoda rozszerzająca podstawowy queryBuilder, zapewniający wyszukiwanie pełnotekstowe.
     * @param builder
     * @param query
     * @return
     */
    protected abstract BoolQueryBuilder extendQueryBuilder(BoolQueryBuilder builder, QUERY query);

    @Override
    public Long listCount(QUERY query) {
        BoolQueryBuilder queryBuilder = createQueryBuilderInternal(query);
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(queryBuilder)
                .build();
        IndexCoordinates indexCoordinates = elasticsearchTemplate.getIndexCoordinatesFor(listClazz);
        return elasticsearchTemplate.count(searchQuery, indexCoordinates);
    }

    /**
     * Metoda zwracająca nazwę indeksu z klasy DTO
     * @return
     */
    protected String getIndexName() {
        Document annotation = listClazz.getAnnotation(Document.class);
        try {
            Method method = annotation.annotationType().getDeclaredMethod("indexName");
            String value = (String) method.invoke(annotation, (Object[])null);
            return value.replace("#{@indexNamePrefix}", elasticSearchParams.getIndexNamePrefix());
        } catch (Exception e) {
            log.error("{}{}", e.getMessage(), e);
        }
        return null;
    }

    public DTO mapEntityToDto(ENTITY entity, boolean withoutNulls) {
        return (DTO) BeanConversionUtil.mapObject(entity, withoutNulls, dtoClazz);
    }

    public ENTITY mapDtoToEntity(DTO dto, boolean withoutNulls) {
        return (ENTITY) BeanConversionUtil.mapObject(dto, withoutNulls, entityClazz);
    }

    /**
     * Metoda pozwala na odtworzenie więzów w konkretnej encji
     * @param ret
     */
    protected void enrichEntity(ENTITY ret) {

    }

    private static Map<String, String> getEscapeCharsMap() {
        Map<String, String> replacementMap = new HashMap<>();
        replacementMap.put("+", "\\+");
        replacementMap.put("-", "\\-");
        replacementMap.put("=", "\\=");
        replacementMap.put("&&", "\\&&");
        replacementMap.put("||", "\\||");
        replacementMap.put("!", "\\!");
        replacementMap.put("(", "\\(");
        replacementMap.put(")", "\\)");
        replacementMap.put("{", "\\{");
        replacementMap.put("}", "\\}");
        replacementMap.put("[", "\\[");
        replacementMap.put("]", "\\]");
        replacementMap.put("^", "\\^");
        replacementMap.put("\"", "\\\"");
        replacementMap.put("~", "\\~");
        replacementMap.put(":", "\\:");
        replacementMap.put("/", "\\/");
        return replacementMap;
    }

    public String escapeSpecialCharacters(String input){
        String ret = input.replace( "\\", "\\\\" );
        int charsCount = getEscapeCharsMap().keySet().size();
        String[] chars = Arrays.copyOf(getEscapeCharsMap().keySet().toArray(), charsCount, String[].class);
        String[] escapedChars = Arrays.copyOf(getEscapeCharsMap().values().toArray(), charsCount, String[].class);
        ret = StringUtils.replaceEach(ret, chars, escapedChars);
        ret = ret.toLowerCase();
        log.debug("*** escapeSpecialCharacters from {} to {}", input, ret);
        return ret;
    }

    public List<String> escapeSpecialCharacters(List<String> arg){
        return arg.stream().map(item -> escapeSpecialCharacters(item)).collect(toList());
    }

    public JpaRepository<ENTITY, ID> getJpaRepository(){
        return null;
    }

    public Slice<ENTITY> reindexPage(Pageable pageable, Class dtoClass, String indexName, String indexType) {
        Slice<ENTITY> slice = getJpaRepository().findAll(pageable);
        List<DTO> dtoList = mapEntityToDtoForBulkReindex(slice.getContent());
        List<IndexQuery> queries = new ArrayList<>();

        for (DTO dto : dtoList) {
            IndexQuery indexQuery = new IndexQuery();
            indexQuery.setId(dto.getId().toString());
            indexQuery.setObject(dto);
//            if (StringUtils.isNotBlank(indexType))
//                indexQuery.setType(getDtoEsIndexType(dtoClass));
            queries.add(indexQuery);
        }
        IndexCoordinates indexCoordinates = elasticsearchTemplate.getIndexCoordinatesFor(listClazz);
        elasticsearchTemplate.bulkIndex(queries, indexCoordinates);
        queries.clear();
        return slice;
    }

    protected List<DTO> mapEntityToDtoForBulkReindex(List<ENTITY> entities) {
        return entities.stream()
                .map(entity -> mapEntityToDto(entity, false))
                .collect(toList()
                );
    }

    public long getTotalRecordsCount(){
        return getJpaRepository().count();
    }

    public  String getDtoEsIndexName(Class<DTO> cls) {
        if (cls.isAnnotationPresent(Document.class)) {
            Document doc =  cls.getAnnotation(Document.class);
            return doc.indexName();
        }
        return null;
    }

//    public  String getDtoEsIndexType(Class<DTO> cls) {
//        if (cls.isAnnotationPresent(Document.class)) {
//            Document doc =  cls.getAnnotation(Document.class);
//            return doc.type();
//        }
//        return null;
//    }

    public  String getNestedPath(Class<?> rootClass, String fullFieldPath) {
        List<String> pathParts = new ArrayList<>(Arrays.asList(fullFieldPath.split("\\.")));
        List<String> nestedPath = new ArrayList<>();
        findNestedPath(rootClass, pathParts, nestedPath, new ArrayList<>());
        return nestedPath.isEmpty() ? null : String.join(".", nestedPath);
    }

    private boolean findNestedPath(Class<?> clazz, List<String> remainingPath, List<String> nestedPath, List<String> currentPath) {
        if (remainingPath.isEmpty()) {
            return false;
        }

        String currentFieldName = remainingPath.remove(0);
        currentPath.add(currentFieldName); // Dodajemy aktualne pole do ścieżki

        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().equals(currentFieldName)) {
                if (field.isAnnotationPresent(org.springframework.data.elasticsearch.annotations.Field.class)) {
                    org.springframework.data.elasticsearch.annotations.Field annotation = field.getAnnotation(org.springframework.data.elasticsearch.annotations.Field.class);
                    if (annotation.type() == FieldType.Nested) {
                        nestedPath.clear();
                        nestedPath.addAll(new ArrayList<>(currentPath)); // Nadpisujemy nestedPath
                    }
                }

                if (!remainingPath.isEmpty()) {
                    return findNestedPath(field.getType(), remainingPath, nestedPath, currentPath);
                }
                return true;
            }
        }

        return false;
    }



}
