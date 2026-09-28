package com.fincontrol.category.repository;

import com.fincontrol.category.entity.CategoryEntity;
import com.fincontrol.category.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {
    List<CategoryEntity> findAllByUserIdOrderByTypeAscNameAsc(UUID userId);
    List<CategoryEntity> findAllByUserIdAndTypeOrderByNameAsc(UUID userId, CategoryType type);
    Optional<CategoryEntity> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByUserIdAndTypeAndNameIgnoreCase(UUID userId, CategoryType type, String name);
}
