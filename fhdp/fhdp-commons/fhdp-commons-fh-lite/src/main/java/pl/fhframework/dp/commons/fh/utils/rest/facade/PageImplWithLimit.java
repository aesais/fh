package pl.fhframework.dp.commons.fh.utils.rest.facade;

import lombok.Getter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

@Getter
public class PageImplWithLimit<T> extends PageImpl<T> {

    private final boolean limitReached;

    public static <T> PageImplWithLimit<T> empty() {
        return empty(Pageable.unpaged());
    }
    static <T> PageImplWithLimit<T> empty(Pageable pageable) {
        return new PageImplWithLimit(Collections.emptyList(), pageable, 0L, false);
    }

    public PageImplWithLimit(List<T> content, Pageable pageable, Long total, boolean limitReached) {
        super(content, pageable, total==null ? 0L : total < 0 ? -total : total);
        this.limitReached = limitReached;
    }

    public PageImplWithLimit(List<T> content, boolean limitReached) {
        super(content);
        this.limitReached = limitReached;
    }

    @Override
    public int getTotalPages() {
        return super.getTotalPages();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.domain.Page#getTotalElements()
     */
    @Override
    public long getTotalElements() {
        if (limitReached){
            return -1*super.getTotalElements();
        }
        return super.getTotalElements();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.domain.Slice#hasNext()
     */
    @Override
    public boolean hasNext() {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.domain.Slice#isLast()
     */
    @Override
    public boolean isLast() {
        return false;
    }
}
