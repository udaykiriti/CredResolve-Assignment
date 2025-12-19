package com.expenseshare.repository;

import com.expenseshare.model.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    @Query("SELECT s FROM Settlement s WHERE s.group.id = :groupId ORDER BY s.createdAt DESC")
    List<Settlement> findByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT s FROM Settlement s WHERE s.payer.id = :userId OR s.payee.id = :userId ORDER BY s.createdAt DESC")
    List<Settlement> findByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM Settlement s WHERE s.group.id = :groupId AND (s.payer.id = :userId OR s.payee.id = :userId) ORDER BY s.createdAt DESC")
    List<Settlement> findByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
