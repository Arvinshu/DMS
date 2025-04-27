/**
 * 文件路径: src/main/java/org/ls/controller/api/ProjectStageApiController.java
 * 开发时间: 2025-04-23
 * 作者: Gemini
 * 代码用途: 提供项目阶段相关的 RESTful API 接口
 */
package org.ls.controller.api;

import jakarta.validation.Valid; // 使用 Jakarta EE 9+ 验证注解
import org.ls.dto.ProjectStageDto;
import org.ls.service.ProjectStageService;
import org.ls.utils.StringUtils; // 引入 StringUtils 用于输入清理
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController // 标记为 REST 控制器，返回 JSON
@RequestMapping("/api/project-stages") // API 的基础路径
public class ProjectStageApiController {

    private static final Logger log = LoggerFactory.getLogger(ProjectStageApiController.class);

    @Autowired // 注入 Service
    private ProjectStageService projectStageService;

    /**
     * 获取项目阶段列表（分页）
     * 用于数据维护界面
     *
     * @param pageNum    页码 (默认为 1)
     * @param pageSize   每页数量 (默认为 10)
     * @param nameFilter 名称过滤条件 (可选)
     * @return ResponseEntity 包含分页数据和总数
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getStages(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String nameFilter) {

        log.info("API request received: Get project stages - pageNum={}, pageSize={}, nameFilter='{}'", pageNum, pageSize, nameFilter);

        // 对输入进行基本的清理，防止 XSS
        String sanitizedFilter = StringUtils.simpleSanitize(nameFilter);

        List<ProjectStageDto> stages = projectStageService.getStages(sanitizedFilter, pageNum, pageSize);
        int totalCount = projectStageService.countStages(sanitizedFilter);

        Map<String, Object> response = new HashMap<>();
        response.put("data", stages);
        response.put("total", totalCount);
        response.put("pageNum", pageNum);
        response.put("pageSize", pageSize);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取所有启用的项目阶段（不分页）
     * 用于项目或任务创建/编辑时的下拉选择
     *
     * @return ResponseEntity 包含启用的项目阶段列表
     */
    @GetMapping("/all-enabled")
    public ResponseEntity<List<ProjectStageDto>> getAllEnabledStages() {
        log.info("API request received: Get all enabled project stages");
        List<ProjectStageDto> stages = projectStageService.getAllEnabledStagesOrdered();
        return ResponseEntity.ok(stages);
    }

    /**
     * 根据 ID 获取单个项目阶段详情
     *
     * @param stageId 阶段 ID
     * @return ResponseEntity 包含项目阶段 DTO 或 404 Not Found
     */
    @GetMapping("/{stageId}")
    public ResponseEntity<ProjectStageDto> getStageById(@PathVariable Integer stageId) {
        log.info("API request received: Get project stage by ID - {}", stageId);
        ProjectStageDto stageDto = projectStageService.getStageById(stageId);
        if (stageDto != null) {
            return ResponseEntity.ok(stageDto);
        } else {
            log.warn("Project stage not found for ID: {}", stageId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 创建新的项目阶段
     *
     * @param projectStageDto 包含新阶段信息的 DTO (经过 @Valid 验证)
     * @return ResponseEntity 包含创建后的项目阶段 DTO 和 201 Created 状态
     */
    @PostMapping
    public ResponseEntity<ProjectStageDto> createStage(@Valid @RequestBody ProjectStageDto projectStageDto) {
        log.info("API request received: Create project stage - Name: {}", projectStageDto.getStageName());
        // 对 DTO 中的字符串字段进行清理
        projectStageDto.setStageName(StringUtils.simpleSanitize(projectStageDto.getStageName()));
        projectStageDto.setStageDescription(StringUtils.simpleSanitize(projectStageDto.getStageDescription()));

        ProjectStageDto createdStage = projectStageService.createStage(projectStageDto);
        // 返回 201 Created 状态和新创建的资源
        return ResponseEntity.status(HttpStatus.CREATED).body(createdStage);
    }

    /**
     * 更新现有的项目阶段
     *
     * @param stageId         要更新的阶段 ID
     * @param projectStageDto 包含更新信息的 DTO (经过 @Valid 验证)
     * @return ResponseEntity 包含更新后的项目阶段 DTO 或 404 Not Found
     */
    @PutMapping("/{stageId}")
    public ResponseEntity<ProjectStageDto> updateStage(
            @PathVariable Integer stageId,
            @Valid @RequestBody ProjectStageDto projectStageDto) {
        log.info("API request received: Update project stage - ID: {}", stageId);
        // 对 DTO 中的字符串字段进行清理
        projectStageDto.setStageName(StringUtils.simpleSanitize(projectStageDto.getStageName()));
        projectStageDto.setStageDescription(StringUtils.simpleSanitize(projectStageDto.getStageDescription()));

        try {
            ProjectStageDto updatedStage = projectStageService.updateStage(stageId, projectStageDto);
            return ResponseEntity.ok(updatedStage);
        } catch (RuntimeException e) {
            // Service 层在找不到 ID 时会抛出异常
            log.warn("Failed to update project stage, ID {} not found or other error: {}", stageId, e.getMessage());
            // 可以根据异常类型返回不同状态码，这里简化处理
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.notFound().build();
            }
            // 其他错误，让全局异常处理器处理或返回 500
            throw e; // 或者 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 删除项目阶段
     *
     * @param stageId 要删除的阶段 ID
     * @return ResponseEntity 无内容 (204 No Content) 或 404 Not Found 或 409 Conflict (如果有关联)
     */
    @DeleteMapping("/{stageId}")
    public ResponseEntity<Void> deleteStage(@PathVariable Integer stageId) {
        log.warn("API request received: Delete project stage - ID: {}", stageId);
        try {
            projectStageService.deleteStage(stageId);
            // 删除成功返回 204 No Content
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            // 处理 Service 层抛出的异常
            log.error("Error deleting project stage with ID: {}", stageId, e);
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("无法删除")) { // 假设 Service 层在有关联时抛出特定消息
                // 返回 409 Conflict 表示资源冲突
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            // 其他未知错误
            throw e; // 或者 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
