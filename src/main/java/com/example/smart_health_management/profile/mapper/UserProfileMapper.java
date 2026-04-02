package com.example.smart_health_management.profile.mapper;

import com.example.smart_health_management.profile.model.UserProfile;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserProfileMapper {

    @Select("""
            SELECT id, user_id, avatar, gender, birthday, region, signature, created_at, updated_at
            FROM user_profile
            WHERE user_id = #{userId}
            LIMIT 1
            """)
    UserProfile findByUserId(Long userId);

    @Insert("""
            INSERT INTO user_profile (user_id, avatar, gender, birthday, region, signature)
            VALUES (#{userId}, #{avatar}, #{gender}, #{birthday}, #{region}, #{signature})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserProfile profile);

    @Update("""
            UPDATE user_profile
            SET avatar = #{avatar}, gender = #{gender}, birthday = #{birthday},
                region = #{region}, signature = #{signature}
            WHERE user_id = #{userId}
            """)
    int updateByUserId(UserProfile profile);

    @Update("UPDATE user_profile SET avatar = #{avatar} WHERE user_id = #{userId}")
    int updateAvatar(Long userId, String avatar);
}
