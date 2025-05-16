/**
 * 文件路径: src/main/java/org/ls/service/impl/ProjectServiceImpl.java
 * 开发时间: 2025-04-24 (更新)
 * 作者: Gemini (更新者)
 * 代码用途: 项目服务实现类，处理核心业务逻辑，包括状态计算和标签关联。
 * 更新内容: 修改 getProjectLookups 方法中获取业务类型、利润中心、员工、工时代码的调用方式，使用现有方法及参数。
 */
package org.ls.service.impl;

import org.ls.dto.*; // 引入所有需要的 DTO
import org.ls.entity.*; // 引入所有需要的实体
import org.ls.mapper.*; // 引入所有需要的 Mapper
import org.ls.service.*; // 引入所有需要的 Service
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils; // 用于检查集合是否为空

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProjectServiceImpl implements ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectServiceImpl.class);

    // 注入 Mappers
    @Autowired private ProjectMapper projectMapper;
    @Autowired private TaskMapper taskMapper;
    @Autowired private ProjectTagMapper projectTagMapper;
    @Autowired private ProjectStageMapper projectStageMapper;

    // 注入 Services
    @Autowired private TaskService taskService;
    @Autowired private ProjectStageService projectStageService;
    @Autowired private BusinessTypeService businessTypeService;
    @Autowired private ProfitCenterService profitCenterService; // 使用上传的 Service
    @Autowired private EmployeeService employeeService;       // 使用上传的 Service
    @Autowired private TimesheetCodeService timesheetCodeService; // 使用上传的 Service


    @Override
    @Transactional
    public ProjectListDto createProject(ProjectCreateDto projectCreateDto, String creatorEmployee) {
        log.info("Creating new project: {}", projectCreateDto.getProjectName());
        Project entity = projectCreateDto.toEntity();
        entity.setProjectId(null); // 确保插入
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        entity.setCreatedBy(creatorEmployee);
        entity.setUpdatedBy(creatorEmployee);

        // 1. 插入项目主体
        int affectedRows = projectMapper.insert(entity);
        if (affectedRows == 0 || entity.getProjectId() == null) {
            log.error("Failed to insert project: {}", projectCreateDto.getProjectName());
            throw new RuntimeException("创建项目失败");
        }
        log.info("Project base info created with ID: {}", entity.getProjectId());

        // 2. 处理标签关联
        if (!CollectionUtils.isEmpty(projectCreateDto.getTagIds())) {
            try {
                projectMapper.insertProjectTags(entity.getProjectId(), projectCreateDto.getTagIds());
                log.info("Associated tags for project ID: {}", entity.getProjectId());
            } catch (Exception e) {
                log.error("Failed to associate tags for project ID: {}", entity.getProjectId(), e);
                throw new RuntimeException("关联项目标签失败", e);
            }
        }

        // 3. 获取刚创建的项目信息（包含标签）并计算状态返回
        Project createdProject = projectMapper.findById(entity.getProjectId());
        List<ProjectTag> tags = projectTagMapper.findTagsByProjectId(entity.getProjectId());
        List<ProjectTagDto> tagDtos = tags.stream().map(ProjectTagDto::fromEntity).collect(Collectors.toList());

        String initialStatus = "待办";
        String initialStageName = null;

        return ProjectListDto.fromEntity(createdProject, tagDtos, initialStageName, initialStatus, null);
    }

    @Override
    @Transactional
    public ProjectListDto updateProject(Long projectId, ProjectCreateDto projectCreateDto, String updaterEmployee) {
        log.info("Updating project with ID: {}", projectId);
        Project existingProject = projectMapper.findById(projectId);
        if (existingProject == null) {
            log.warn("Project not found for ID: {}", projectId);
            throw new RuntimeException("项目不存在，无法更新");
        }

        Project entityToUpdate = projectCreateDto.toEntity();
        entityToUpdate.setProjectId(projectId);
        entityToUpdate.setUpdatedAt(OffsetDateTime.now());
        entityToUpdate.setUpdatedBy(updaterEmployee);

        int affectedRows = projectMapper.updateById(entityToUpdate);
        if (affectedRows == 0) {
            log.warn("Project update affected 0 rows for ID: {}", projectId);
        }
        log.info("Project base info updated for ID: {}", projectId);

        try {
            projectMapper.deleteProjectTagsByProjectId(projectId);
            if (!CollectionUtils.isEmpty(projectCreateDto.getTagIds())) {
                projectMapper.insertProjectTags(projectId, projectCreateDto.getTagIds());
            }
            log.info("Updated tags association for project ID: {}", projectId);
        } catch (Exception e) {
            log.error("Failed to update tags association for project ID: {}", projectId, e);
            throw new RuntimeException("更新项目标签关联失败", e);
        }

        Project updatedProject = projectMapper.findById(projectId);
        List<ProjectTag> tags = projectTagMapper.findTagsByProjectId(projectId);
        List<ProjectTagDto> tagDtos = tags.stream().map(ProjectTagDto::fromEntity).collect(Collectors.toList());

        Map<String, Object> latestTaskInfo = taskService.findLatestTaskInfoByProjectId(projectId);
        String currentStatus = calculateProjectStatus(latestTaskInfo, projectId);
        String currentStageName = getStageNameFromTaskInfo(latestTaskInfo);

        return ProjectListDto.fromEntity(updatedProject, tagDtos, currentStageName, currentStatus, null);
    }

    @Override
    @Transactional
    public void deleteProject(Long projectId) {
        log.warn("Attempting to delete project with ID: {}", projectId);
        Project existingProject = projectMapper.findById(projectId);
        if (existingProject == null) {
            log.warn("Project not found for ID: {}, skipping deletion.", projectId);
            return;
        }

        int taskCount = projectMapper.countTasksByProjectId(projectId);
        if (taskCount > 0) {
            log.error("Cannot delete project ID: {} because it has {} associated tasks.", projectId, taskCount);
            throw new RuntimeException("无法删除项目，请先删除项目下的所有任务。");
        }

        projectMapper.deleteProjectTagsByProjectId(projectId);
        int affectedRows = projectMapper.deleteById(projectId);
        if (affectedRows == 0) {
            log.error("Failed to delete project with ID: {}", projectId);
            throw new RuntimeException("删除项目失败");
        }
        log.info("Project deleted successfully for ID: {}", projectId);
    }

    @Override
    public ProjectDetailDto getProjectDetailById(Long projectId) {
        log.debug("Fetching project detail for ID: {}", projectId);
        Project project = projectMapper.findById(projectId);
        if (project == null) {
            return null;
        }

        List<ProjectTag> tags = projectTagMapper.findTagsByProjectId(projectId);
        List<ProjectTagDto> tagDtos = tags.stream().map(ProjectTagDto::fromEntity).collect(Collectors.toList());
        List<TaskDto> taskDtos = taskService.getTasksByProjectId(projectId);

        Map<String, Object> latestTaskInfo = taskService.findLatestTaskInfoByProjectId(projectId);
        String currentStatus = calculateProjectStatus(latestTaskInfo, projectId);
        String currentStageName = getStageNameFromTaskInfo(latestTaskInfo);
        Object progress = null; // TODO: Calculate progress

        return ProjectDetailDto.fromEntity(project, tagDtos, taskDtos, currentStageName, currentStatus, progress);
    }

    @Override
    public Map<String, Object> searchProjects(Map<String, Object> searchParams, int pageNum, int pageSize) {
        log.info("Searching projects with params: {}, page: {}, size: {}", searchParams, pageNum, pageSize);

        int offset = (pageNum - 1) * pageSize;
        searchParams.put("limit", pageSize);
        searchParams.put("offset", offset);
        List<Long> tagIds = (List<Long>) searchParams.get("tagIds");
        if (!CollectionUtils.isEmpty(tagIds)) {
            searchParams.put("tagCount", tagIds.size());
        } else {
            searchParams.remove("tagCount");
        }

        List<Project> projects = projectMapper.findProjectsByCriteria(searchParams);
        int totalCount = projectMapper.countProjectsByCriteria(searchParams);

        List<ProjectListDto> resultList = projects.stream().map(project -> {
            List<ProjectTag> projectTags = projectTagMapper.findTagsByProjectId(project.getProjectId());
            List<ProjectTagDto> tagDtos = projectTags.stream().map(ProjectTagDto::fromEntity).collect(Collectors.toList());
            Map<String, Object> latestTaskInfo = taskService.findLatestTaskInfoByProjectId(project.getProjectId());
            String currentStatus = calculateProjectStatus(latestTaskInfo, project.getProjectId());
            String currentStageName = getStageNameFromTaskInfo(latestTaskInfo);
            Object progress = null; // TODO: Calculate progress
            return ProjectListDto.fromEntity(project, tagDtos, currentStageName, currentStatus, progress);
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("data", resultList);
        response.put("total", totalCount);
        response.put("pageNum", pageNum);
        response.put("pageSize", pageSize);

        return response;
    }

    @Override
    public Map<String, Object> getProjectLookups() {
        log.debug("Fetching lookups for project creation/search");
        Map<String, Object> lookups = new HashMap<>();

        // --- Business Types ---
        try {
            Map<String, Object> btParams = Map.of("enabled", true); // Filter for enabled=true
            List<BusinessType> activeBusinessTypes = businessTypeService.findBusinessTypes(btParams);
            // Convert to DTO (Assuming BusinessTypeDto exists or create a simple one)
            List<Map<String, Object>> businessTypeLookups = activeBusinessTypes.stream()
                    .filter(bt -> bt != null && bt.getBusinessName() != null)
                    .map(bt -> Map.<String, Object>of("value", bt.getBusinessName(), "text", bt.getBusinessName())) // Simple map for dropdown
                    .collect(Collectors.toList());
            lookups.put("businessTypes", businessTypeLookups);
        } catch (Exception e) {
            log.error("Error fetching business types lookup", e);
            lookups.put("businessTypes", Collections.emptyList()); // Provide empty list on error
        }

        // --- Profit Centers ---
        try {
            Map<String, Object> pcParams = Map.of("enabled", true); // Filter for enabled=true
            List<ProfitCenter> activeProfitCenters = profitCenterService.findProfitCenterDistinctCZRAll(pcParams);
            // Convert to DTO or simple map for dropdown
            List<Map<String, Object>> profitCenterLookups = activeProfitCenters.stream()
                    .filter(pc -> pc != null && pc.getZone() != null && pc.getCustomZoneRemark() != null)
                    .map(pc -> Map.<String, Object>of("value", pc.getZone(), "text", pc.getCustomZoneRemark())) // Use zone as value, remark as text
                    .collect(Collectors.toList());
            lookups.put("profitCenters", profitCenterLookups);
        } catch (Exception e) {
            log.error("Error fetching profit centers lookup", e);
            lookups.put("profitCenters", Collections.emptyList());
        }

        // --- Employees ---
        try {
            Map<String, Object> empParams = Map.of("active", true); // Filter for active=true
            List<Employee> activeEmployees = employeeService.findEmployees(empParams);
            // Convert to DTO or simple map for dropdown
            List<Map<String, Object>> employeeLookups = activeEmployees.stream()
                    .filter(emp -> emp != null && emp.getEmployee() != null && emp.getEmployeeName() != null)
                    .map(emp -> Map.<String, Object>of("value", emp.getEmployee(), "text", emp.getEmployeeName() + " (" + emp.getEmployeeId() + ")")) // value=employee, text=name(id)
                    .collect(Collectors.toList());
            lookups.put("employees", employeeLookups);
        } catch (Exception e) {
            log.error("Error fetching employees lookup", e);
            lookups.put("employees", Collections.emptyList());
        }

        // --- Timesheet Codes (Project specific) ---
        try {
            Map<String, Object> tsParams = Map.of(
                    "projectTimesheet", true, // Filter for is_project_timesheet=true
                    "enabled", true          // Filter for is_enabled=true
            );
            List<TimesheetCode> activeProjectTsCodes = timesheetCodeService.findTimesheetCodes(tsParams);
            // Convert to DTO or simple map for dropdown
            List<Map<String, Object>> timesheetCodeLookups = activeProjectTsCodes.stream()
                    .filter(ts -> ts != null && ts.getTsBm() != null)
                    .map(ts -> Map.<String, Object>of("value", ts.getTsBm(), "text", ts.getTsBm() + (ts.getCustomProjectName() != null ? " - " + ts.getCustomProjectName() : ""))) // value=ts_bm, text=ts_bm - custom_name
                    .collect(Collectors.toList());
            lookups.put("timesheetCodes", timesheetCodeLookups);
        } catch (Exception e) {
            log.error("Error fetching timesheet codes lookup", e);
            lookups.put("timesheetCodes", Collections.emptyList());
        }

        // --- Project Tags ---
        try {
            lookups.put("tags", projectTagMapper.findAllEnabledTags().stream()
                    .map(ProjectTagDto::fromEntity).collect(Collectors.toList())); // Assuming ProjectTagDto is needed
            // Or simple map: .map(tag -> Map.<String, Object>of("value", tag.getTagId(), "text", tag.getTagName()))
        } catch (Exception e) {
            log.error("Error fetching project tags lookup", e);
            lookups.put("tags", Collections.emptyList());
        }

        // --- Project Stages ---
        try {
            lookups.put("stages", projectStageService.getAllEnabledStagesOrdered()); // Already returns List<ProjectStageDto>
            // Or simple map: .map(stage -> Map.<String, Object>of("value", stage.getStageId(), "text", stage.getStageName()))
        } catch (Exception e) {
            log.error("Error fetching project stages lookup", e);
            lookups.put("stages", Collections.emptyList());
        }

        return lookups;
    }

    // --- Private Helper Methods ---

    // 用以在页面中展示项目状态的计算逻辑实现。
    private String calculateProjectStatus(Map<String, Object> latestTaskInfo, Long projectId) {
        if (latestTaskInfo == null) {
            if (taskMapper.countTasksByProjectId(projectId) == 0) {
                return "待办";
            } else {
                log.warn("Could not find latest task info for project {}, but tasks exist. Defaulting status to '进行中'.", projectId);
                return "进行中";
            }
        }
        String latestStatus = (String) latestTaskInfo.get("task_status");
        return switch (latestStatus) {
            case "已暂停" -> "已暂停";
            case "已取消" -> "已取消";
            case "已完成" -> "已完成";
            case "待办" -> "待办";
            default -> "进行中";
        };
    }

    private String getStageNameFromTaskInfo(Map<String, Object> latestTaskInfo) {
        if (latestTaskInfo != null && latestTaskInfo.get("stage_id") != null) {
            try {
                Integer stageId = (Integer) latestTaskInfo.get("stage_id");
                ProjectStage stage = projectStageMapper.findById(stageId);
                return (stage != null) ? stage.getStageName() : null;
            } catch (Exception e) {
                log.error("Error getting stage name for stage_id from task info: {}", latestTaskInfo.get("stage_id"), e);
                return null;
            }
        }
        return null;
    }
}
