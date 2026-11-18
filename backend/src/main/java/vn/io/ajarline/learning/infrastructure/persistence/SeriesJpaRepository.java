package vn.io.ajarline.learning.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SeriesJpaRepository extends JpaRepository<SeriesEntity, UUID> {
}
