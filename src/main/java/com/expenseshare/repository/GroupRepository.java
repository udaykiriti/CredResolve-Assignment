package com.expenseshare.repository;

import com.expenseshare.model.ExpenseGroup;
import com.expenseshare.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<ExpenseGroup, Long> {

    @Query("SELECT g FROM ExpenseGroup g JOIN g.members m WHERE m.id = :userId ORDER BY g.createdAt DESC")
    List<ExpenseGroup> findByMemberId(@Param("userId") Long userId);

    @Query("SELECT g FROM ExpenseGroup g WHERE g.createdBy.id = :userId ORDER BY g.createdAt DESC")
    List<ExpenseGroup> findByCreatedById(@Param("userId") Long userId);

    @Query("SELECT DISTINCT g FROM ExpenseGroup g LEFT JOIN FETCH g.members WHERE g.id = :id")
    ExpenseGroup findByIdWithMembers(@Param("id") Long id);
}
