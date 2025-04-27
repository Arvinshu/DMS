/**
 * 文件路径: src/main/java/org/ls/service/impl/ProjectStageServiceImpl.java
 * 开发时间: 2025-04-23
 * 作者: Gemini
 * 代码用途: 项目阶段服务实现类
 */
package org.ls.service.impl;

import org.ls.dto.ProjectStageDto;
import org.ls.entity.ProjectStage;
import org.ls.mapper.ProjectStageMapper;
import org.ls.service.ProjectStageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException; // 用于捕获外键约束异常
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 引入事务注解

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service // 标记为 Spring Service 组件
public class ProjectStageServiceImpl implements ProjectStageService {

    private static final Logger log = LoggerFactory.getLogger(ProjectStageServiceImpl.class);

    @Autowired // 自动注入 Mapper
    private ProjectStageMapper projectStageMapper;

    @Override
    @Transactional // 添加事务管理
    public ProjectStageDto createStage(ProjectStageDto projectStageDto) {
        log.info("Creating new project stage: {}", projectStageDto.getStageName());
        ProjectStage entity = projectStageDto.toEntity();
        // 清除 ID，确保是插入操作
        entity.setStageId(null);
        // Service 层设置创建和更新时间戳（虽然数据库有默认值，但明确设置更佳）
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        int affectedRows = projectStageMapper.insert(entity);
        if (affectedRows == 0) {
            log.error("Failed to insert project stage: {}", projectStageDto.getStageName());
            throw new RuntimeException("创建项目阶段失败");
        }
        log.info("Project stage created successfully with ID: {}", entity.getStageId());
        // 返回包含生成 ID 的 DTO
        return ProjectStageDto.fromEntity(entity);
    }

    @Override
    @Transactional // 添加事务管理
    public ProjectStageDto updateStage(Integer stageId, ProjectStageDto projectStageDto) {
        log.info("Updating project stage with ID: {}", stageId);
        ProjectStage existingStage = projectStageMapper.findById(stageId);
        if (existingStage == null) {
            log.warn("Project stage not found for ID: {}", stageId);
            throw new RuntimeException("项目阶段不存在，无法更新");
        }

        ProjectStage entityToUpdate = projectStageDto.toEntity();
        entityToUpdate.setStageId(stageId); // 确保 ID 正确
        entityToUpdate.setUpdatedAt(OffsetDateTime.now()); // 设置更新时间

        int affectedRows = projectStageMapper.updateById(entityToUpdate);
        if (affectedRows == 0) {
            log.error("Failed to update project stage with ID: {}", stageId);
            // 可能并发修改导致，或者 ID 不存在（虽然前面检查过）
            throw new RuntimeException("更新项目阶段失败");
        }
        log.info("Project stage updated successfully for ID: {}", stageId);
        // 返回更新后的完整信息
        return ProjectStageDto.fromEntity(projectStageMapper.findById(stageId));
    }

    @Override
    @Transactional // 添加事务管理
    public void deleteStage(Integer stageId) {
        log.warn("Attempting to delete project stage with ID: {}", stageId);
        ProjectStage existingStage = projectStageMapper.findById(stageId);
        if (existingStage == null) {
            log.warn("Project stage not found for ID: {}, skipping deletion.", stageId);
            // 或者可以抛出异常，取决于业务要求
            return;
            // throw new RuntimeException("项目阶段不存在，无法删除");
        }

        try {
            int affectedRows = projectStageMapper.deleteById(stageId);
            if (affectedRows == 0) {
                log.error("Failed to delete project stage with ID: {}", stageId);
                throw new RuntimeException("删除项目阶段失败");
            }
            log.info("Project stage deleted successfully for ID: {}", stageId);
        } catch (DataIntegrityViolationException e) {
            // 捕获可能的外键约束异常 (如果 t_task 的外键设置为 RESTRICT)
            log.error("Cannot delete project stage with ID: {} due to existing references (e.g., tasks).", stageId, e);
            throw new RuntimeException("无法删除项目阶段，可能存在关联的任务。请先处理关联任务。", e);
        }
    }

    @Override
    public ProjectStageDto getStageById(Integer stageId) {
        log.debug("Fetching project stage by ID: {}", stageId);
        ProjectStage entity = projectStageMapper.findById(stageId);
        return ProjectStageDto.fromEntity(entity);
    }

    @Override
    public List<ProjectStageDto> getStages(String nameFilter, int pageNum, int pageSize) {
        log.debug("Fetching project stages with filter: '{}', page: {}, size: {}", nameFilter, pageNum, pageSize);
        int offset = (pageNum - 1) * pageSize;
        List<ProjectStage> entities = projectStageMapper.findAll(nameFilter, pageSize, offset);
        // 将实体列表转换为 DTO 列表
        return entities.stream()
                .map(ProjectStageDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public int countStages(String nameFilter) {
        log.debug("Counting project stages with filter: '{}'", nameFilter);
        return projectStageMapper.countAll(nameFilter);
    }

    @Override
    public List<ProjectStageDto> getAllEnabledStagesOrdered() {
        log.debug("Fetching all enabled and ordered project stages");
        List<ProjectStage> entities = projectStageMapper.findAllEnabledOrdered();
        return entities.stream()
                .map(ProjectStageDto::fromEntity)
                .collect(Collectors.toList());
    }
}
