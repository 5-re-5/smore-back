package org.oreo.smore.global.common;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter @NoArgsConstructor
public class CursorPage<T> {
    private Long cursorId;
    private int size;
    private List<T> content;
    private boolean hasNext;
    private Long nextCursor;

    public CursorPage(Long cursorId, int size) {
        this.cursorId = cursorId;
        this.size = size;
    }

    public static <T extends Identifiable> CursorPage<T> of(List<T> result, int size) {
        boolean hasNext = result.size() > size;
        List<T> pageContent = hasNext ? result.subList(0, size) : result;
        Long nextCursor = pageContent.isEmpty() ? null : pageContent.get(pageContent.size() - 1).getId();
        CursorPage<T> page = new CursorPage<>();
        page.setContent(pageContent);
        page.setHasNext(hasNext);
        page.setNextCursor(nextCursor);
        return page;
    }

    public Long getCursorIdOrDefault() {
        return cursorId != null ? cursorId : Long.MAX_VALUE;
    }

    public interface Identifiable {
        Long getId();
    }
}
