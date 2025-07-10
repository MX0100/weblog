package io.github.mx0100.weblog.dto.response;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Page response DTO
 * 
 * @param <T> content type
 * @author mx0100
 */
@Data
public class PageResponse<T> {
    
    private List<T> content;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
    private Boolean first;
    private Boolean last;
    
    /**
     * Create PageResponse from Spring Data Page
     * 
     * @param page Spring Data Page
     * @param <T> content type
     * @return PageResponse
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        PageResponse<T> response = new PageResponse<>();
        response.setContent(page.getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setFirst(page.isFirst());
        response.setLast(page.isLast());
        return response;
    }
} 