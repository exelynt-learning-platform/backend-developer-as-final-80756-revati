package com.booking.service;

import com.booking.domain.entity.Resource;
import com.booking.domain.repository.ResourceRepository;
import com.booking.dto.request.ResourceRequest;
import com.booking.dto.response.PageResponse;
import com.booking.dto.response.ResourceResponse;
import com.booking.exception.ResourceNotFoundException;
import com.booking.mapper.ResourceMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final ResourceMapper resourceMapper;

    public ResourceService(ResourceRepository resourceRepository, ResourceMapper resourceMapper) {
        this.resourceRepository = resourceRepository;
        this.resourceMapper = resourceMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<ResourceResponse> findAll(int page, int size, String sort) {
        Pageable pageable = buildPageable(page, size, sort);
        Page<Resource> result = resourceRepository.findAll(pageable);
        return new PageResponse<>(
                result.map(resourceMapper::toResponse).getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );
    }

    @Transactional(readOnly = true)
    public ResourceResponse findById(Long id) {
        return resourceMapper.toResponse(getResource(id));
    }

    public ResourceResponse create(ResourceRequest request) {
        Resource resource = resourceMapper.toEntity(request);
        return resourceMapper.toResponse(resourceRepository.save(resource));
    }

    public ResourceResponse update(Long id, ResourceRequest request) {
        Resource resource = getResource(id);
        resourceMapper.apply(request, resource);
        return resourceMapper.toResponse(resourceRepository.save(resource));
    }

    public void delete(Long id) {
        Resource resource = getResource(id);
        resourceRepository.delete(resource);
    }

    public Resource getResource(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }

    private Pageable buildPageable(int page, int size, String sort) {
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        }
        String[] parts = sort.split(",");
        String property = parts[0].trim();
        Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, property));
    }
}
