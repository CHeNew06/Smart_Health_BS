package com.example.smart_health_management.auth.mapper;

import com.example.smart_health_management.auth.model.UserAccount;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserAccountMapper {

    @Select("""
            SELECT id, account, password_hash, email, nickname, status, created_at, updated_at
            FROM app_user
            WHERE account = #{account}
            LIMIT 1
            """)
    UserAccount findByAccount(String account);

    @Select("""
            SELECT id, account, password_hash, email, nickname, status, created_at, updated_at
            FROM app_user
            WHERE id = #{id}
            LIMIT 1
            """)
    UserAccount findById(Long id);

    @Select("""
            SELECT id, account, password_hash, email, nickname, status, created_at, updated_at
            FROM app_user
            WHERE email = #{email}
            LIMIT 1
            """)
    UserAccount findByEmail(String email);

    @Insert("""
            INSERT INTO app_user (account, password_hash, email, nickname, status)
            VALUES (#{account}, #{passwordHash}, #{email}, #{nickname}, 1)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserAccount user);

    @Update("UPDATE app_user SET nickname = #{nickname} WHERE id = #{id}")
    int updateNickname(Long id, String nickname);

    @Update("UPDATE app_user SET password_hash = #{passwordHash} WHERE id = #{id}")
    int updatePassword(Long id, String passwordHash);
}

