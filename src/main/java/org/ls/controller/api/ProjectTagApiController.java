/**
 * 文件路径: src/main/java/org/ls/controller/api/ProjectTagApiController.java
 * 开发时间: 2025-04-23
 * 作者: Gemini
 * 代码用途: 提供项目标签相关的 RESTful API 接口
 */
package org.ls.controller.api;

import jakarta.validation.Valid;
import org.ls.dto.ProjectTagDto;
import org.ls.service.ProjectTagService;
import org.ls.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/project-tags") // API 的基础路径
public class ProjectTagApiController {

    private static final Logger log = LoggerFactory.getLogger(ProjectTagApiController.class);

    @Autowired
    private ProjectTagService projectTagService;

    /**
     * 获取项目标签列表（分页）
     * 用于数据维护界面
     *
     * @param pageNum    页码 (默认为 1)
     * @param pageSize   每页数量 (默认为 10)
     * @param nameFilter 名称过滤条件 (可选)
     * @return ResponseEntity 包含分页数据和总数
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getTags(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String nameFilter) {

        log.info("API request received: Get project tags - pageNum={}, pageSize={}, nameFilter='{}'", pageNum, pageSize, nameFilter);
        String sanitizedFilter = StringUtils.simpleSanitize(nameFilter);

        List<ProjectTagDto> tags = projectTagService.getTags(sanitizedFilter, pageNum, pageSize);
        int totalCount = projectTagService.countTags(sanitizedFilter);

        Map<String, Object> response = new HashMap<>();
        response.put("data", tags);
        response.put("total", totalCount);
        response.put("pageNum", pageNum);
        response.put("pageSize", pageSize);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取所有项目标签（不分页）
     * 用于项目创建/编辑时的下拉选择
     *
     * @return ResponseEntity 包含所有项目标签列表
     */
    @GetMapping("/all") // 使用 /all 区分分页查询
    public ResponseEntity<List<ProjectTagDto>> getAllTags() {
        log.info("API request received: Get all project tags");
        List<ProjectTagDto> tags = projectTagService.getAllTags();
        return ResponseEntity.ok(tags);
    }

    /**
     * 根据 ID 获取单个项目标签详情
     *
     * @param tagId 标签 ID
     * @return ResponseEntity 包含项目标签 DTO 或 404 Not Found
     */
    @GetMapping("/{tagId}")
    public ResponseEntity<ProjectTagDto> getTagById(@PathVariable Long tagId) {
        log.info("API request received: Get project tag by ID - {}", tagId);
        ProjectTagDto tagDto = projectTagService.getTagById(tagId);
        if (tagDto != null) {
            return ResponseEntity.ok(tagDto);
        } else {
            log.warn("Project tag not found for ID: {}", tagId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 创建新的项目标签
     *
     * @param projectTagDto 包含新标签信息的 DTO (经过 @Valid 验证)
     * @return ResponseEntity 包含创建后的项目标签 DTO 和 201 Created 状态
     */
    @PostMapping
    public ResponseEntity<ProjectTagDto> createTag(@Valid @RequestBody ProjectTagDto projectTagDto) {
        log.info("API request received: Create project tag - Name: {}", projectTagDto.getTagName());
        projectTagDto.setTagName(StringUtils.simpleSanitize(projectTagDto.getTagName()));

        try {
            ProjectTagDto createdTag = projectTagService.createTag(projectTagDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTag);
        } catch (RuntimeException e) {
            // 处理 Service 层可能抛出的唯一约束异常
            log.error("Error creating project tag: {}", e.getMessage());
            if (e.getMessage().contains("已存在")) {
                // 返回 409 Conflict
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            throw e; // 其他错误交由全局处理器
        }
    }

    /**
     * 更新现有的项目标签
     *
     * @param tagId         要更新的标签 ID
     * @param projectTagDto 包含更新信息的 DTO (经过 @Valid 验证)
     * @return ResponseEntity 包含更新后的项目标签 DTO 或 404 Not Found 或 409 Conflict
     */
    @PutMapping("/{tagId}")
    public ResponseEntity<ProjectTagDto> updateTag(
            @PathVariable Long tagId,
            @Valid @RequestBody ProjectTagDto projectTagDto) {
        log.info("API request received: Update project tag - ID: {}", tagId);
        projectTagDto.setTagName(StringUtils.simpleSanitize(projectTagDto.getTagName()));

        try {
            ProjectTagDto updatedTag = projectTagService.updateTag(tagId, projectTagDto);
            return ResponseEntity.ok(updatedTag);
        } catch (RuntimeException e) {
            log.warn("Failed to update project tag, ID {} or other error: {}", tagId, e.getMessage());
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("已存在")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            throw e;
        }
    }

    /**
     * 删除项目标签
     *
     * @param tagId 要删除的标签 ID
     * @return ResponseEntity 无内容 (204 No Content) 或 404 Not Found
     */
    @DeleteMapping("/{tagId}")
    public ResponseEntity<Void> deleteTag(@PathVariable Long tagId) {
        log.warn("API request received: Delete project tag - ID: {}", tagId);
        try {
            projectTagService.deleteTag(tagId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            // Service 层在找不到 ID 时会记录日志并返回，这里无需特殊处理 404
            // 如果 Service 层修改为抛出异常，则需要在这里捕获并返回 404
            log.error("Error deleting project tag with ID: {}", tagId, e);
            // 假设所有 Service 层的删除失败都视为内部错误
            throw e; // 或者 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
