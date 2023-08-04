package com.allst.springboot.mapper;

import com.allst.springboot.entity.QuartzJob;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Hutu
 * @since 2023-05-30 下午 09:40
 */
@Mapper
@Component
public interface QuartzMapper extends BaseMapper<QuartzJob> {
    /**
     * 查询所有调度任务
     *
     * @return 调度任务列表
     */
    @Select("SELECT * FROM quartz_job")
    public List<QuartzJob> selectJobAll();

    /**
     * 通过调度ID查询调度任务信息
     *
     * @param jobId 调度ID
     *
     * @return 角色对象信息
     */
    @Select("SELECT * FROM quartz_job WHERE job_id = #{jobId}")
    public QuartzJob selectJobById(Long jobId);

    /**
     * 通过调度ID删除调度任务信息
     *
     * @param jobId 调度ID
     *
     * @return 结果
     */
    @Delete("DELETE FROM quartz_job WHERE job_id = #{jobId}")
    public int deleteJobById(Long jobId);

    @Update("UPDATE quartz_job SET status = #{status} WHERE job_id = #{jobId}")
    public int changeStatus(Long jobId, String status);
}
