package com.expenseshare.repository;

import com.expenseshare.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("SELECT e FROM Expense e WHERE e.group.id = :groupId ORDER BY e.createdAt DESC")
    List<Expense> findByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.splits WHERE e.group.id = :groupId ORDER BY e.createdAt DESC")
    List<Expense> findByGroupIdWithSplits(@Param("groupId") Long groupId);

    @Query("SELECT e FROM Expense e LEFT JOIN FETCH e.splits WHERE e.id = :id")
    Expense findByIdWithSplits(@Param("id") Long id);

    @Query("SELECT e FROM Expense e WHERE e.paidBy.id = :userId ORDER BY e.createdAt DESC")
    List<Expense> findByPaidById(@Param("userId") Long userId);

    void deleteByGroupId(Long groupId);
}
