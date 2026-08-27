package com.bank.lms.repository;

import com.bank.lms.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 知识库 Repository。
 *
 * 注意：实体继承 BaseEntity，带 @Where(is_deleted=0)，故 findAll/findByTitle 等
 * SELECT 会自动过滤已软删记录；@Modifying 的 UPDATE 为批量写，不受 @Where 影响。
 */
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    /** 查询某标题下的所有切块（已自动过滤软删） */
    List<KnowledgeBase> findByTitle(String title);

    /** 软删某标题下所有切块，返回影响行数 */
    @Modifying(clearAutomatically = true)
    @Query("update KnowledgeBase k set k.isDeleted = 1 where k.title = :title")
    int softDeleteByTitle(@Param("title") String title);

    /** 按标题聚合列表：title / category / 最近更新时间 / 块数 */
    @Query("select k.title, k.category, max(k.updatedAt), count(k) " +
           "from KnowledgeBase k group by k.title, k.category order by max(k.updatedAt) desc")
    List<Object[]> listGrouped();
}
