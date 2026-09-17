package com.example.resourcebooking.resource;

import com.example.resourcebooking.common.NotFoundException;
import com.example.resourcebooking.common.SortUtil;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceService {
    private static final Set<String> SORT_FIELDS = Set.of("id", "name", "price");
    private final ResourceRepository resources;

    public ResourceService(ResourceRepository resources) {
        this.resources = resources;
    }

    @Transactional(readOnly = true)
    public Page<ResourceResponse> findAll(int page, int size, String sort) {
        return resources.findAll(SortUtil.page(page, size, sort, SORT_FIELDS)).map(ResourceResponse::from);
    }

    @Transactional(readOnly = true)
    public ResourceResponse findById(Long id) {
        return ResourceResponse.from(get(id));
    }

    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        return ResourceResponse.from(resources.save(new Resource(request.name(), request.description(), request.price())));
    }

    @Transactional
    public ResourceResponse update(Long id, ResourceRequest request) {
        Resource resource = get(id);
        resource.update(request.name(), request.description(), request.price());
        return ResourceResponse.from(resource);
    }

    @Transactional
    public void delete(Long id) {
        resources.delete(get(id));
    }

    public Resource get(Long id) {
        return resources.findById(id).orElseThrow(() -> new NotFoundException("Resource not found: " + id));
    }
}
