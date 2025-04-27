/**
 * 文件路径: src/main/java/org/ls/service/impl/ProjectTagServiceImpl.java
 * 开发时间: 2025-04-23
 * 作者: Gemini
 * 代码用途: 项目标签服务实现类
 */
package org.ls.service.impl;

import org.ls.dto.ProjectTagDto;
import org.ls.entity.ProjectTag;
import org.ls.mapper.ProjectMapper; // 需要注入 ProjectMapper 来处理关联删除
import org.ls.mapper.ProjectTagMapper;
import org.ls.service.ProjectTagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service // 标记为 Spring Service 组件
public class ProjectTagServiceImpl implements ProjectTagService {

    private static final Logger log = LoggerFactory.getLogger(ProjectTagServiceImpl.class);

    @Autowired // 自动注入 Mapper
    private ProjectTagMapper projectTagMapper;

    @Autowired // 注入 ProjectMapper 以便删除标签时处理关联 (虽然外键设置了级联，但显式处理更安全)
    private ProjectMapper projectMapper; // 注意：ProjectMapper 尚未创建，此处仅为占位符

    @Override
    @Transactional // 添加事务管理
    public ProjectTagDto createTag(ProjectTagDto projectTagDto) {
        log.info("Creating new project tag: {}", projectTagDto.getTagName());
        ProjectTag entity = projectTagDto.toEntity();
        // 清除 ID，确保是插入操作
        entity.setTagId(null);
        // Service 层设置创建和更新时间戳
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        try {
            int affectedRows = projectTagMapper.insert(entity);
            if (affectedRows == 0) {
                log.error("Failed to insert project tag: {}", projectTagDto.getTagName());
                throw new RuntimeException("创建项目标签失败");
            }
            log.info("Project tag created successfully with ID: {}", entity.getTagId());
            return ProjectTagDto.fromEntity(entity);
        } catch (DataIntegrityViolationException e) {
            // 捕获可能的唯一约束冲突 (tag_name UNIQUE)
            log.error("Failed to insert project tag due to constraint violation (likely duplicate name '{}').", projectTagDto.getTagName(), e);
            throw new RuntimeException("创建项目标签失败，标签名称可能已存在。", e);
        }
    }

    @Override
    @Transactional // 添加事务管理
    public ProjectTagDto updateTag(Long tagId, ProjectTagDto projectTagDto) {
        log.info("Updating project tag with ID: {}", tagId);
        ProjectTag existingTag = projectTagMapper.findById(tagId);
        if (existingTag == null) {
            log.warn("Project tag not found for ID: {}", tagId);
            throw new RuntimeException("项目标签不存在，无法更新");
        }

        ProjectTag entityToUpdate = projectTagDto.toEntity();
        entityToUpdate.setTagId(tagId); // 确保 ID 正确
        entityToUpdate.setUpdatedAt(OffsetDateTime.now()); // 设置更新时间

        try {
            int affectedRows = projectTagMapper.updateById(entityToUpdate);
            if (affectedRows == 0) {
                log.error("Failed to update project tag with ID: {}", tagId);
                throw new RuntimeException("更新项目标签失败");
            }
            log.info("Project tag updated successfully for ID: {}", tagId);
            return ProjectTagDto.fromEntity(projectTagMapper.findById(tagId));
        } catch (DataIntegrityViolationException e) {
            // 捕获可能的唯一约束冲突 (tag_name UNIQUE)
            log.error("Failed to update project tag ID {} due to constraint violation (likely duplicate name '{}').", tagId, projectTagDto.getTagName(), e);
            throw new RuntimeException("更新项目标签失败，标签名称可能已存在。", e);
        }
    }

    @Override
    @Transactional // 添加事务管理
    public void deleteTag(Long tagId) {
        log.warn("Attempting to delete project tag with ID: {}", tagId);
        ProjectTag existingTag = projectTagMapper.findById(tagId);
        if (existingTag == null) {
            log.warn("Project tag not found for ID: {}, skipping deletion.", tagId);
            return;
            // throw new RuntimeException("项目标签不存在，无法删除");
        }

        try {
            // 尽管 t_project_project_tag 设置了 ON DELETE CASCADE,
            // 但如果需要更复杂的清理逻辑（例如通知相关项目），可以在这里添加。
            // 目前直接依赖数据库的级联删除。

            int affectedRows = projectTagMapper.deleteById(tagId);
            if (affectedRows == 0) {
                log.error("Failed to delete project tag with ID: {}", tagId);
                throw new RuntimeException("删除项目标签失败");
            }
            log.info("Project tag deleted successfully for ID: {}", tagId);
        } catch (DataIntegrityViolationException e) {
            // 这个异常理论上不应该发生，因为关联表会级联删除。
            // 但如果发生，记录日志。
            log.error("Unexpected error deleting project tag with ID: {}. Possible constraint issue despite CASCADE.", tagId, e);
            throw new RuntimeException("删除项目标签时发生意外错误。", e);
        }
    }

    @Override
    public ProjectTagDto getTagById(Long tagId) {
        log.debug("Fetching project tag by ID: {}", tagId);
        ProjectTag entity = projectTagMapper.findById(tagId);
        return ProjectTagDto.fromEntity(entity);
    }

    @Override
    public List<ProjectTagDto> getTags(String nameFilter, int pageNum, int pageSize) {
        log.debug("Fetching project tags with filter: '{}', page: {}, size: {}", nameFilter, pageNum, pageSize);
        int offset = (pageNum - 1) * pageSize;
        List<ProjectTag> entities = projectTagMapper.findAll(nameFilter, pageSize, offset);
        return entities.stream()
                .map(ProjectTagDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public int countTags(String nameFilter) {
        log.debug("Counting project tags with filter: '{}'", nameFilter);
        return projectTagMapper.countAll(nameFilter);
    }

    @Override
    public List<ProjectTagDto> getAllTags() {
        log.debug("Fetching all project tags");
        List<ProjectTag> entities = projectTagMapper.findAllEnabledTags(); // Mapper 方法名保持一致
        return entities.stream()
                .map(ProjectTagDto::fromEntity)
                .collect(Collectors.toList());
    }
}
