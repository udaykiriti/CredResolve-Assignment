package com.expenseshare.repository;

import com.expenseshare.model.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, Long> {

    @Query("SELECT es FROM ExpenseSplit es WHERE es.user.id = :userId")
    List<ExpenseSplit> findByUserId(@Param("userId") Long userId);

    @Query("SELECT es FROM ExpenseSplit es WHERE es.expense.group.id = :groupId")
    List<ExpenseSplit> findByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT es FROM ExpenseSplit es WHERE es.expense.group.id = :groupId AND es.user.id = :userId")
    List<ExpenseSplit> findByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
