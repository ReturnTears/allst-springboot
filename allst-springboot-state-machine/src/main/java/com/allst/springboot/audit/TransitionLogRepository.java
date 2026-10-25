package com.allst.springboot.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransitionLogRepository extends JpaRepository<TransitionLog, Long> {

    boolean existsByMachineTypeAndMachineIdAndEventId(String machineType, String machineId, String eventId);

    List<TransitionLog> findByMachineTypeAndMachineIdOrderByIdAsc(String machineType, String machineId);
}
