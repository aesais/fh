package pl.fhframework.dp.transport.dto.changes;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import pl.fhframework.dp.commons.base.comparator.ChangeTypeEnum;
import pl.fhframework.dp.commons.base.comparator.annotations.ComparableClass;
import pl.fhframework.dp.commons.base.comparator.annotations.ComparableCollection;
import pl.fhframework.dp.commons.base.comparator.annotations.ComparableField;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Slf4j
public abstract class ObjectDataComparatorBase<CHANGE, DTO> {

    protected final static String BEAUTIFY_TRUE = "$.common.true";
    protected final static String BEAUTIFY_FALSE = "$.common.false";
    protected final static String ADDED = "$.compare.added";
    protected final static String DELETED = "$.compare.deleted";
    final static DateTimeFormatter dtf = DateTimeFormatter.ofPattern(getDateFormat());
    final static DateTimeFormatter dtmf = DateTimeFormatter.ofPattern(getDateTimeFormat());

    List<CHANGE> changes = null;
    public List<CHANGE> getChanges(){
        return changes;
    }

    protected QualifiesForCompare qualifiesForCompare = null;

    /**
     *
     */
    public static class ObjectDataInfo {
        public Object before;
        public Object after;
        public final String rootName;
        public String xPath;

        public Field field;

        /** There is @CompareAnnotatedOnly - causing all fields to have annotations to be compared */
        public boolean annotatedOnly = false;

        public ObjectDataInfo(String rootName){
            this.rootName = rootName;
        }

        public boolean ignoreAdded(){
            if (field == null){
                return false;
            }
            ComparableField cf = field.getAnnotation(ComparableField.class);
            return (cf!=null && Arrays.asList(cf.ignore()).contains(ChangeTypeEnum.ADDED));
        }

        public boolean ignoreDeleted(){
            if (field == null){
                return false;
            }
            ComparableField cf = field.getAnnotation(ComparableField.class);
            return (cf!=null && Arrays.asList(cf.ignore()).contains(ChangeTypeEnum.DELETED));
        }

        public boolean ignoreModified(){
            if (field == null){
                return false;
            }
            ComparableField cf = field.getAnnotation(ComparableField.class);
            return (cf!=null && Arrays.asList(cf.ignore()).contains(ChangeTypeEnum.MODIFIED));
        }
    }

    /**
     * Class to provide method to match objects in collections.
     * Usually it is enough to provide getSequenceIdFunction implementation
     */
    public static class CollectionPathInfo {
        /**
         * For some collection get method to get object in collection based on object
         * If null value is returned then natural order in collection will be used (but might not make sense)
         * @return
         */
        public Function<Object,Object> getSequenceIdFunction(){
            return o -> (Integer)o.hashCode();
        }
        /**
         * Return null when default can be used (default is just comparing Comparables fromn the function above)
         * @return
         */
        public Comparator<Object> getComparator(){
            return null;
        }
        /**
         * Standard collection pointer
         * @param object
         * @param counter
         *
         * @return
         */
        public String getPointer(ObjectDataInfo object, int counter){
            return ""+counter;
        }
    }
    protected Map<String, CollectionPathInfo> registeredCollectionClasses = new HashMap<String, CollectionPathInfo>();

    protected CollectionPathInfo createCollectionPathInfo(){
        return new CollectionPathInfo();
    }

    protected void afterCompareFixChanges(){}

    protected CollectionPathInfo getPathInfo(String path, ObjectDataInfo obInfo) {
        String spath = path.replaceAll("\\[[0-9]*\\]", "");
        CollectionPathInfo pi = registeredCollectionClasses.get(spath);
        if (pi == null){
            pi = createCollectionPathInfo();
            registeredCollectionClasses.put(spath, pi);
        }
        return pi;
    }

    public enum FieldTypeEnum{
        FIELD,
        CLASS,
        COLLECTION
    }
    public static class QualifiesForCompare {
        private boolean recursive;
        public QualifiesForCompare(boolean recursive){
            this.recursive = recursive;
        }

        protected boolean fieldQualifies(String fieldName, String className, String parentPath){
            return true;
        }
        protected boolean fieldQualifiesAsClass(String fieldName, String className, String parentPath){
            return true;
        }

        /**
         * Check if field in base object should be compared
         * @param field
         * @return
         */
        public FieldTypeEnum qualifies(ObjectDataInfo obInfo, Field field){
            Class fieldClass = field.getType();
            if (!fieldQualifies(field.getName(), fieldClass.getName(), obInfo.xPath)){
                return null;
            }
            if (field.isAnnotationPresent(ComparableField.class)) {
                if (fieldClass.isPrimitive()) {
                    return FieldTypeEnum.FIELD;
                }
                if (recursive){
                    if (fieldQualifiesAsClass(field.getName(), fieldClass.getName(), obInfo.xPath)){
                        return FieldTypeEnum.CLASS;
                    } else {
                        return FieldTypeEnum.FIELD;
                    }
                } else {
                    return FieldTypeEnum.FIELD;
                }
            }
            if (fieldClass.isAnnotationPresent(ComparableClass.class)){
                return FieldTypeEnum.CLASS;
            }
            if (field.isAnnotationPresent(ComparableCollection.class)){
                return FieldTypeEnum.COLLECTION;
            }
            if (obInfo.annotatedOnly){
                return null;
            }
            if (Collection.class.isAssignableFrom(fieldClass)){
                return FieldTypeEnum.COLLECTION;
            }
            if(fieldClass.isEnum() || field.isEnumConstant()) {
                return FieldTypeEnum.FIELD;
            }
            // in recursive mode we compare every field - if it is our package then we compare as class
            if (fieldQualifiesAsClass(field.getName(), fieldClass.getName(), obInfo.xPath)){
                return FieldTypeEnum.CLASS;
            } else {
                return FieldTypeEnum.FIELD;
            }
        }
    }

    public List<CHANGE> compareAndGetChanges(DTO before, DTO after) {
        compareDeclarationObjects(before, after);
        return getChanges();
    }

    public void compareDeclarationObjects(DTO before, DTO after) {

        changes = new LinkedList<>();
        ObjectDataInfo obDataInfo = new ObjectDataInfo("/");
        obDataInfo.before = before;
        obDataInfo.after = after;
        obDataInfo.xPath = "";
        obDataInfo.annotatedOnly = true; // only in main element

        compareObject(obDataInfo);
        afterCompareFixChanges();
    }

    public void compareObject(ObjectDataInfo obInfo) {

        Object newObj = obInfo.after;
        Object oldObj = obInfo.before;

        String rootName = obInfo.rootName;
        String xPath = obInfo.xPath;
        String xPathPrefix = getXpathSeparator() + xPath;

        if (oldObj == null && newObj == null) {
            return;
        }
        if (oldObj == null) {
            if (!obInfo.ignoreAdded()){
                if (newObj instanceof CompareSummaryData){
                    changes.add(newChange(xPathPrefix, ADDED, ((CompareSummaryData)newObj).buildSummary()));
                } else {
                    registerFields(newObj, xPathPrefix, true, obInfo);
                }
            }
            return;
        }
        if (newObj == null) {
            if (!obInfo.ignoreDeleted()){
                if (oldObj instanceof CompareSummaryData){
                    changes.add(newChange(xPathPrefix, ((CompareSummaryData)oldObj).buildSummary(), DELETED));
                } else {
                    registerFields(oldObj, xPathPrefix, false, obInfo);
                }
            }
            return;
        }
        log.debug("Objects "+ obInfo.xPath+"/"+(obInfo.field==null?"":obInfo.field.getName())+" will be compared (class "+newObj.getClass()+")");

        Class<? extends Object> clazz = newObj.getClass();

        ArrayList<Field> allFields = new ArrayList<Field>();
        Class<? extends Object> ccc=clazz;
        while (ccc!=null){
            if (ccc.getAnnotation(ComparableClass.class)!=null || qualifiesForCompare.recursive){
                allFields.addAll(Arrays.asList(ccc.getDeclaredFields()));
            }
            ccc = ccc.getSuperclass();
        }

        for (Field field : allFields) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            ObjectDataInfo obInfoField = new ObjectDataInfo(rootName);

            ComparableField cf = field.getAnnotation(ComparableField.class);
            ComparableClass cc = field.getType().getAnnotation(ComparableClass.class);
            String cfXPath = (cf==null || StringUtils.isBlank(cf.Xpath()))? (cc==null ? field.getName() :cc.Xpath()):cf.Xpath();

            FieldTypeEnum qualifiesField = qualifiesForCompare.qualifies(obInfo, field);

            if (qualifiesField == FieldTypeEnum.CLASS) {
                log.debug("Field "+field.getName()+" located: "+xPath+" is of Comparable Class");
                try {
                    obInfoField.before = field.get(oldObj);
                    obInfoField.after = field.get(newObj);
                } catch (IllegalAccessException e) {
                    log.error("Can't access value of field : " + field.getName(),e);
                    return;
                }

                obInfoField.xPath = xPath + cfXPath + getXpathSeparator();
                compareObject(obInfoField);
            } else if (qualifiesField == FieldTypeEnum.FIELD) {
                try {
                        log.debug("Comparing field : " + field.getName());
                        String path = null;
                        if (cf == null){
                            path = xPathPrefix + "" + field.getName();
                        } else if (cf.rawXpath() == false) {
                            path = xPathPrefix + "" + cf.Xpath();
                        } else {
                            path = xPathPrefix + field.getName();
                        }
                        if (cf != null && !cf.inSchema()){
                            path += " (obj)";
                        }

                        Object valueAfter = null;
                        Object valueBefore = null;

                        String nm =field.getType().getName();
                        if (nm.equals("double")){
                            valueAfter = field.getDouble(newObj);
                            valueBefore = field.getDouble(oldObj);
                        }else if (nm.equals("int")){
                            valueAfter = field.getInt(newObj);
                            valueBefore = field.getInt(oldObj);
                        }else if (nm.equals("boolean")){
                            valueAfter = field.getBoolean(newObj);
                            valueBefore = field.getBoolean(oldObj);
                        }else if (nm.equals("short")){
                            valueAfter = field.getShort(newObj);
                            valueBefore = field.getShort(oldObj);
                        }else if (nm.equals("long")){
                            valueAfter = field.getLong(newObj);
                            valueBefore = field.getLong(oldObj);
                        }else if (nm.equals("byte")){
                            valueAfter = field.getByte(newObj);
                            valueBefore = field.getByte(oldObj);
                        }else if (nm.equals("java.time.LocalDate")){
                            valueAfter = field.get(newObj);
                            valueBefore = field.get(oldObj);
                        }else{
                            valueAfter = field.get(newObj);
                            valueBefore = field.get(oldObj);
                        }

                        Object valueBeforeI = valueBefore;
                        Object valueAfterI = valueAfter;

                        String valueBeforeStr = "";
                        if (valueBefore!=null){
                            if (valueBefore instanceof BigDecimal && cf!=null && cf.precision()>-1){
                                valueBeforeI = ((BigDecimal)valueBefore).setScale(cf.precision());
                                valueBeforeStr = ((BigDecimal)valueBeforeI).toPlainString();
                            } else {
                                valueBeforeStr = valueBefore.toString().trim();
                            }
                        }
                        String valueAfterStr = "";
                        if (valueAfter!=null){
                            if (valueAfter instanceof BigDecimal && cf!=null && cf.precision()>-1){
                                valueAfterI = ((BigDecimal)valueAfter).setScale(cf.precision());
                                valueAfterStr = ((BigDecimal)valueAfterI).toPlainString();
                            } else {
                                valueAfterStr = valueAfter.toString().trim();
                            }
                        }
                        if ((valueAfter == null && valueBefore == null) || valueBeforeStr.equals(valueAfterStr))
                            continue;
                        else if (valueAfter == null) {
                            if (!obInfoField.ignoreDeleted()){
                                CHANGE change = newChange(path, beautify(valueBeforeI, field.getName()), "");
                                changes.add(change);
                            }

                        } else if (valueBefore == null) {
                            if (!obInfoField.ignoreAdded()){
                                CHANGE change = newChange(path, "", beautify(valueAfterI, field.getName()));
                                changes.add(change);
                            }

                        } else if (!valueBefore.equals(valueAfter)) {
                            if (!obInfoField.ignoreModified()){
                                boolean notEqualButSameAfterComparison = false;
                                if (valueBefore instanceof BigDecimal && valueAfter instanceof BigDecimal) {
                                    notEqualButSameAfterComparison = (0 == ((BigDecimal) valueBefore).compareTo((BigDecimal) valueAfter));
                                }
                                if (valueBefore instanceof Date && valueAfter instanceof Date) {
                                    notEqualButSameAfterComparison = (0 == ((Date) valueBefore).compareTo((Date) valueAfter));
                                }
                                if (!notEqualButSameAfterComparison) {
                                    CHANGE change = newChange(path, beautify(valueBeforeI, field.getName()), beautify(valueAfterI, field.getName()));
                                    changes.add(change);
                                }
                            }
                        }
                } catch (IllegalAccessException iae) {
                    log.error("Can't access value of field : " + field.getName());
                }
            } else if (qualifiesField == FieldTypeEnum.COLLECTION) {
                try {
                    log.debug("Comparing collection : " + field.getName());
                    Collection<Object> newValue = (Collection<Object>) field.get(newObj);
                    Collection<Object> oldValue = (Collection<Object>) field.get(oldObj);
                    obInfo.before = oldValue;
                    obInfo.after = newValue;
                    ComparableCollection coll = field.getAnnotation(ComparableCollection.class);
                    obInfo.xPath = xPath + (coll==null ? field.getName() : coll.Xpath());
                    compareObjectsCollection(oldValue, newValue, obInfo);
                } catch (IllegalAccessException iae) {
                    log.error("Can't access value of field : " + field.getName());
                }
            }
        }
    }

    protected void registerFields(Object obj, String xPathPrefix, boolean added, ObjectDataInfo obInfo) {
        Field[] fields = obj.getClass().getDeclaredFields();
        for(Field field: fields) {
            try {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (qualifiesForCompare.qualifies(obInfo, field) == null){
                    continue;
                }
                field.setAccessible(true);
                if(field.get(obj) != null) {
                    Object value = field.get(obj);
                    String className = value.getClass().getName();
                    if(value.toString().startsWith(className + "@")) {
                        registerFields(value, xPathPrefix + field.getName() + getXpathSeparator(), added, obInfo);
                    } else if (value instanceof List) {
                        int counter = 1;
                        for(Object el: (List)value) {
                            String xpathPrefix = xPathPrefix + field.getName() + "[" + counter++ + "]" + getXpathSeparator();
                            registerFields(el, xpathPrefix , added, obInfo);
                        }
                    } else {
                        if(added) {
                            changes.add(newChange(xPathPrefix + field.getName(), ADDED, beautify(field.get(obj), field.getName())));
                        } else {
                            changes.add(newChange(xPathPrefix + field.getName(), beautify(field.get(obj), field.getName()), DELETED));
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                log.error("Can't access value of field : " + field.getName());
            }
        }
    }

    protected abstract String getXpathSeparator();

    protected abstract CHANGE newChange(String xPath, String operation, String summary);

    private void compareObjectsCollection(Collection<Object> oldObjCollection,
                                          Collection<Object> newObjCollection,
                                          ObjectDataInfo obInfo) {
        if (oldObjCollection == null && newObjCollection == null) {
            return;
        }
        CollectionPathInfo pi = getPathInfo(obInfo.xPath, obInfo);
        Function<Object,Object> seqIdFunction = pi.getSequenceIdFunction();

        Map<Object, Object> oldValues = new HashMap<Object, Object>();
        Map<Object, Object> newValues = new HashMap<Object, Object>();
        Set<Object> keys = new HashSet<Object>();
        if (oldObjCollection != null) {
            for (Object item : oldObjCollection) {
                Object iID = seqIdFunction.apply(item);
                oldValues.put(iID, item);
                keys.add(iID);
            }
        }
        if (newObjCollection != null) {
            for (Object item : newObjCollection) {
                Object iID = seqIdFunction.apply(item);
                newValues.put(iID, item);
                keys.add(iID);
            }
        }

        Comparator<Object> idComparator = pi.getComparator();
        if (idComparator == null){
            idComparator = (firstId, secondId) -> {
                if (firstId != null && secondId != null){
                    if(firstId instanceof Integer && secondId instanceof Integer) {
                        return ((Integer) firstId).compareTo((Integer) secondId);
                    } else if (firstId instanceof String && secondId instanceof String) {
                        return ((String) firstId).compareTo((String) secondId);
                    } else if (firstId instanceof Long && secondId instanceof Long) {
                        return ((Long) firstId).compareTo((Long) secondId);
                    }
                } else if (firstId == null && secondId != null){
                    return 1;
                } else if (firstId != null){
                    return -1;
                }
                return -1;//0 eliminates from the list
            };
        }

        Object[] keysArray = keys.toArray();
        Arrays.sort(keysArray, idComparator);
        int counter = 0;
        for (Object currentId : keysArray) {
            counter++;
            ObjectDataInfo newObjectDataInfo = new ObjectDataInfo(obInfo.rootName);
            newObjectDataInfo.before = oldValues.get(currentId);
            newObjectDataInfo.after = newValues.get(currentId);
            String pointer = pi.getPointer(newObjectDataInfo, counter);
            if(pointer==null) {
                pointer = String.valueOf(counter);
            }
            newObjectDataInfo.xPath = obInfo.xPath + "[" + pointer + "]" + getXpathSeparator();
            compareObject(newObjectDataInfo);
        }
    }

    protected String beautify(Object o, SimpleDateFormat sdf, String fieldName) {
        if (o == null) {
            return null;
        }
        if (o instanceof Boolean) {
            Boolean b = (Boolean) o;
            if (b != null && b.booleanValue() == true) {
                return BEAUTIFY_TRUE;
            } else {
                return BEAUTIFY_FALSE;
            }
            //}else if(o instanceof DateTime){
            //    if(sdf==null){
            //        return (new SimpleDateFormat(BEAUTIFY_DATETIME)).format((java.util.Date) o);
            //    }else{
            //        return sdf.format((java.util.Date) o);
            //    }
        } else if (o instanceof LocalDate) {
            if (sdf == null) {
                return (((LocalDate) o).format(dtf));
            } else {
                return sdf.format((Date) o);
            }

        } else if (o instanceof LocalDateTime) {
            if (sdf == null) {
                return (((LocalDateTime) o).format(dtmf));
            } else {
                return sdf.format((Date) o);
            }

        } else if (o instanceof Enum) {
            o = "enum." + o.getClass().getTypeName() + "." + o.toString();
        }
        return o.toString();
    }

    protected static String getDateFormat() {
        return "dd.MM.yyyy";
    }

    protected static String getDateTimeFormat() {
        return "dd.MM.yyyy HH:mm:ss";
    }

    private String beautify(Object o, String fieldName){
        return beautify(o,null, fieldName);
    }
}
