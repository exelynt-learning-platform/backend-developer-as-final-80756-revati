package com.booking.mapper;

import com.booking.domain.entity.Resource;
import com.booking.dto.request.ResourceRequest;
import com.booking.dto.response.ResourceResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ResourceMapper {

    public Resource toEntity(ResourceRequest request) {
        Resource resource = new Resource();
        apply(request, resource);
        return resource;
    }

    public void apply(ResourceRequest request, Resource resource) {
        resource.setName(request.getName());
        resource.setType(request.getType());
        resource.setDescription(request.getDescription());
        resource.setBasePrice(request.getBasePrice());
        resource.setAvailable(Boolean.TRUE.equals(request.getAvailable()));
        resource.setUpdatedAt(Instant.now());
    }

    public ResourceResponse toResponse(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getType(),
                resource.getDescription(),
                resource.getBasePrice(),
                resource.isAvailable(),
                resource.getCreatedAt(),
                resource.getUpdatedAt()
        );
    }
}
