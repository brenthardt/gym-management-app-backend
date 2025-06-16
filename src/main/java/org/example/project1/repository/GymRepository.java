package org.example.project1.repository;

import org.example.project1.entity.Gym;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface GymRepository extends JpaRepository<Gym, UUID> {

}
