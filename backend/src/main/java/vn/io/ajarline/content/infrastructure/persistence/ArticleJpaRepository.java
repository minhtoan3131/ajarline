package vn.io.ajarline.content.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ArticleJpaRepository extends JpaRepository<ArticleEntity, UUID> {
}
