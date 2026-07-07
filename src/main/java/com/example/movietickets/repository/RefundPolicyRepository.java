package com.example.movietickets.repository;

import com.example.movietickets.entity.RefundPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundPolicyRepository extends JpaRepository<RefundPolicy, Long> {

    List<RefundPolicy> findAllByOrderByCutoffHoursBeforeShowDesc();
}
