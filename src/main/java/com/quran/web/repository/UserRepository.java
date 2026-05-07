package com.quran.web.repository;

import com.quran.web.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.username = :identifier OR u.email = :identifier")
    Optional<User> findByUsernameOrEmail(@Param("identifier") String identifier);

    @Modifying
    @Query("""
        UPDATE User u
        SET u.lastReadSurahNumber = :surahNumber,
            u.lastReadAyahNumber  = :ayahNumber,
            u.lastReadUpdatedAt   = CURRENT_TIMESTAMP
        WHERE u.id = :userId
    """)
    void updateLastRead(@Param("userId") Long userId,
                        @Param("surahNumber") Integer surahNumber,
                        @Param("ayahNumber") Integer ayahNumber);

    @Modifying
    @Query("UPDATE User u SET u.lastLogin = CURRENT_TIMESTAMP WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE User u SET u.lastLogin = CURRENT_TIMESTAMP, u.lastLoginIp = :ip WHERE u.username = :username")
    void updateLastLoginWithIp(@Param("username") String username, @Param("ip") String ip);
}
