/**
 * 文件路径: src/main/java/org/ls/controller/api/ProjectApiController.java
 * 开发时间: 2025-04-24
 * 作者: Gemini
 * 代码用途: 提供项目相关的 RESTful API 接口
 */
package org.ls.controller.api;

import jakarta.validation.Valid;
import org.ls.dto.ProjectCreateDto;
import org.ls.dto.ProjectDetailDto;
import org.ls.dto.ProjectListDto;
import org.ls.service.ProjectService;
import org.ls.utils.StringUtils; // For input sanitization
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
@RequestMapping("/api/projects") // Base path for project-related APIs
public class ProjectApiController {

    private static final Logger log = LoggerFactory.getLogger(ProjectApiController.class);

    @Autowired
    private ProjectService projectService;

    /**
     * 搜索项目列表（分页）
     *
     * @param pageNum             页码 (默认为 1)
     * @param pageSize            每页数量 (默认为 10)
     * @param nameFilter          项目名称过滤 (可选)
     * @param businessTypeName    业务类型过滤 (可选)
     * @param profitCenterZone    利润中心过滤 (可选)
     * @param projectManager      负责人过滤 (可选)
     * @param tsBm                工时代码过滤 (可选)
     * @param tagIds              标签 ID 列表过滤 (可选, AND logic)
     * @param sortBy              排序字段 (可选, e.g., "name", "createdAt")
     * @param sortOrder           排序顺序 (可选, "ASC" or "DESC")
     * @return ResponseEntity 包含分页数据和总数
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> searchProjects(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize, // 前端需求是50，但API保持灵活性
            @RequestParam(required = false) String nameFilter,
            @RequestParam(required = false) String businessTypeName,
            @RequestParam(required = false) String profitCenterZone,
            @RequestParam(required = false) String projectManager,
            @RequestParam(required = false) String tsBm,
            @RequestParam(required = false) List<Long> tagIds, // Spring Boot can map comma-separated values or repeated params
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder) {

        log.info("API request received: Search projects - pageNum={}, pageSize={}, nameFilter='{}', tags={}",
                pageNum, pageSize, nameFilter, tagIds);

        // Prepare search parameters map for service layer
        Map<String, Object> searchParams = new HashMap<>();
        searchParams.put("nameFilter", StringUtils.simpleSanitize(nameFilter));
        searchParams.put("businessTypeName", StringUtils.simpleSanitize(businessTypeName));
        searchParams.put("profitCenterZone", StringUtils.simpleSanitize(profitCenterZone));
        searchParams.put("projectManager", StringUtils.simpleSanitize(projectManager));
        searchParams.put("tsBm", StringUtils.simpleSanitize(tsBm));
        if (tagIds != null && !tagIds.isEmpty()) {
            searchParams.put("tagIds", tagIds);
        }
        searchParams.put("sortBy", StringUtils.simpleSanitize(sortBy)); // Sanitize sort field
        // Validate sortOrder or default
        String validSortOrder = ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) ? sortOrder.toUpperCase() : "DESC";
        searchParams.put("sortOrder", validSortOrder);


        Map<String, Object> response = projectService.searchProjects(searchParams, pageNum, pageSize);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取用于项目创建/搜索的下拉选项数据
     *
     * @return ResponseEntity 包含各种下拉列表数据
     */
    @GetMapping("/lookups")
    public ResponseEntity<Map<String, Object>> getLookups() {
        log.info("API request received: Get project lookups");
        Map<String, Object> lookups = projectService.getProjectLookups();
        return ResponseEntity.ok(lookups);
    }


    /**
     * 根据 ID 获取项目详情
     *
     * @param projectId 项目 ID
     * @return ResponseEntity 包含项目详情 DTO 或 404 Not Found
     */
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDetailDto> getProjectById(@PathVariable Long projectId) {
        log.info("API request received: Get project by ID - {}", projectId);
        ProjectDetailDto projectDto = projectService.getProjectDetailById(projectId);
        if (projectDto != null) {
            return ResponseEntity.ok(projectDto);
        } else {
            log.warn("Project not found for ID: {}", projectId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 创建新项目
     *
     * @param projectCreateDto 包含新项目信息的 DTO (经过 @Valid 验证)
     * @return ResponseEntity 包含创建后的项目列表 DTO 和 201 Created 状态
     */
    @PostMapping
    public ResponseEntity<ProjectListDto> createProject(@Valid @RequestBody ProjectCreateDto projectCreateDto) {
        // TODO: Get current logged-in user information for creatorEmployee
        String creatorEmployee = "E0001-Gemini"; // Placeholder - Replace with actual user info
        log.info("API request received: Create project - Name: {}, Creator: {}", projectCreateDto.getProjectName(), creatorEmployee);

        // Sanitize string inputs
        projectCreateDto.setProjectName(StringUtils.simpleSanitize(projectCreateDto.getProjectName()));
        projectCreateDto.setProjectDescription(StringUtils.simpleSanitize(projectCreateDto.getProjectDescription()));
        // Other fields like businessTypeName, profitCenterZone etc. usually come from dropdowns, less critical to sanitize here

        try {
            ProjectListDto createdProject = projectService.createProject(projectCreateDto, creatorEmployee);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdProject);
        } catch (RuntimeException e) {
            log.error("Error creating project: {}", e.getMessage());
            if (e.getMessage().contains("已存在")) { // Handle potential unique constraint violation
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            throw e; // Let global handler deal with other errors
        }
    }

    /**
     * 更新现有项目
     *
     * @param projectId        要更新的项目 ID
     * @param projectCreateDto 包含更新信息的 DTO (经过 @Valid 验证)
     * @return ResponseEntity 包含更新后的项目列表 DTO 或 404 Not Found
     */
    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectListDto> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectCreateDto projectCreateDto) {
        // TODO: Get current logged-in user information for updaterEmployee
        String updaterEmployee = "E0001-Gemini"; // Placeholder - Replace with actual user info
        log.info("API request received: Update project - ID: {}, Updater: {}", projectId, updaterEmployee);

        // Sanitize string inputs
        projectCreateDto.setProjectName(StringUtils.simpleSanitize(projectCreateDto.getProjectName()));
        projectCreateDto.setProjectDescription(StringUtils.simpleSanitize(projectCreateDto.getProjectDescription()));

        try {
            ProjectListDto updatedProject = projectService.updateProject(projectId, projectCreateDto, updaterEmployee);
            return ResponseEntity.ok(updatedProject);
        } catch (RuntimeException e) {
            log.warn("Failed to update project, ID {} not found or other error: {}", projectId, e.getMessage());
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("已存在")) { // Handle potential unique constraint violation
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            throw e;
        }
    }

    /**
     * 删除项目
     *
     * @param projectId 要删除的项目 ID
     * @return ResponseEntity 无内容 (204 No Content) 或 404 Not Found 或 409 Conflict (如果项目下有任务)
     */
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId) {
        log.warn("API request received: Delete project - ID: {}", projectId);
        try {
            projectService.deleteProject(projectId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error deleting project with ID: {}", projectId, e);
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("无法删除")) { // Service throws this if tasks exist
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            throw e;
        }
    }
}
