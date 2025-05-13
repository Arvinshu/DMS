/**
 * 文件路径: src/main/java/org/ls/service/impl/ProjectStatisticsServiceImpl.java
 * 开发时间: 2025-05-10 19:35:05 UTC/GMT+08:00
 * 作者: Gemini
 * 代码用途: 项目统计服务实现类，提供具体的统计数据获取和处理逻辑。
 */
package org.ls.service.impl;

import org.ls.dto.stats.*;
import org.ls.entity.Project;
import org.ls.entity.Task;
import org.ls.mapper.ProjectMapper;
import org.ls.mapper.TaskMapper;
import org.ls.service.ProjectStatisticsService;
import org.ls.utils.DateUtils; // 假设此类及方法已存在
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProjectStatisticsServiceImpl implements ProjectStatisticsService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectStatisticsServiceImpl.class);

    // 任务状态常量 (建议定义在公共常量类中)
    private static final String TASK_STATUS_TODO = "待办";
    private static final String TASK_STATUS_IN_PROGRESS = "进行中";
    private static final String TASK_STATUS_COMPLETED = "已完成";
    private static final String TASK_STATUS_CANCELLED = "已取消";
    private static final String TASK_STATUS_PAUSED = "已暂停"; // 如果有此状态

    // 任务优先级常量
    private static final String TASK_PRIORITY_HIGH = "High";

    // 风险等级常量
    private static final String RISK_LEVEL_HIGH = "高";
    private static final String RISK_LEVEL_MEDIUM = "中";
    private static final String RISK_LEVEL_LOW = "低";
    private static final String RISK_LEVEL_NONE = "无";


    private final TaskMapper taskMapper;
    private final ProjectMapper projectMapper;
    private final DateUtils dateUtils; // 假设注入

    @Autowired
    public ProjectStatisticsServiceImpl(TaskMapper taskMapper, ProjectMapper projectMapper, DateUtils dateUtils) {
        this.taskMapper = taskMapper;
        this.projectMapper = projectMapper;
        this.dateUtils = dateUtils;
    }

    @Override
    public List<String> getDistinctTaskAssignees() {
        try {
            return taskMapper.selectDistinctAssignees();
        } catch (Exception e) {
            logger.error("获取去重任务负责人列表失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public DepartmentOverviewStatsDto getDepartmentOverviewStats(String dateRangeString, String projectTypeDimension) {
        DateUtils.DateRange parsedDateRange = dateUtils.parseDateRange(dateRangeString); // 假设返回对象包含 startDate 和 endDate
        LocalDate startDate = parsedDateRange.getStartDate();
        LocalDate endDate = parsedDateRange.getEndDate();

        KpiDto kpis = new KpiDto();
        List<ChartDataPointDto> projectStatusDistributionChart = new ArrayList<>();
        List<EmployeeLoadDto> employeeLoadChart = new ArrayList<>();
        List<ChartDataPointDto> projectTypeDistributionChart = new ArrayList<>();

        try {
            // 1. 计算KPIs
            // 活跃项目数: 统计指定日期范围内创建的，且当前并非“已完成”或“已取消”状态的项目
            // 这个逻辑比较复杂，因为项目状态是动态计算的。
            // 简化：统计在此日期范围内创建的，并且其下有“待办”或“进行中”任务的项目数量。
            // 或者，统计所有非“已完成”/“已取消”的项目（不考虑创建日期，仅表示当前活跃）。
            // 此处采用后者简化逻辑：获取所有非最终态的项目。
            Map<String, Object> activeProjectParams = new HashMap<>();
            // 假设 projectMapper.selectActiveProjectsForRiskAssessment 能够基于某种定义（如项目下有未完成任务）返回活跃项目
            // 并且 ProjectMapper 中有一个 countActiveProjects 方法
            // int activeProjectCount = projectMapper.countActiveProjects(activeProjectParams); // 假设有此方法
            // kpis.setActiveProjects(activeProjectCount);
            // 由于没有直接的 project_status 列，活跃项目数暂时通过任务来间接反映或留空
            kpis.setActiveProjects(calculateActiveProjectsCount(startDate, endDate));


            Map<String, Object> taskParams = new HashMap<>();
            taskParams.put("statusList", List.of(TASK_STATUS_IN_PROGRESS));
            // taskParams.put("createdDateStart", startDate); // 根据需求决定KPI是否受dateRange影响
            // taskParams.put("createdDateEnd", endDate);
            kpis.setInProgressTasks(taskMapper.countTasksByCriteria(taskParams));

            taskParams.clear();
            taskParams.put("isOverdue", true);
            kpis.setTotalOverdueTasks(taskMapper.countTasksByCriteria(taskParams));

            taskParams.clear();
            taskParams.put("statusList", List.of(TASK_STATUS_COMPLETED));
            taskParams.put("updatedDateStart", startDate); // 按任务完成时间（更新时间）
            taskParams.put("updatedDateEnd", endDate);
            kpis.setCompletedTasksThisPeriod(taskMapper.countTasksByCriteria(taskParams));

            // 2. 项目状态分布 (简化模拟)
            // 实际中需要遍历项目，根据任务判断项目状态
            projectStatusDistributionChart.add(ChartDataPointDto.builder().label(TASK_STATUS_IN_PROGRESS).value(kpis.getInProgressTasks()).build()); // 粗略用进行中任务数代表
            projectStatusDistributionChart.add(ChartDataPointDto.builder().label(TASK_STATUS_TODO).value(taskMapper.countTasksByCriteria(Map.of("statusList", List.of(TASK_STATUS_TODO)))).build());
            projectStatusDistributionChart.add(ChartDataPointDto.builder().label(TASK_STATUS_COMPLETED).value(taskMapper.countTasksByCriteria(Map.of("statusList", List.of(TASK_STATUS_COMPLETED)))).build());


            // 3. 员工任务负载
            Map<String, Object> employeeLoadParams = new HashMap<>();
            employeeLoadParams.put("statusList", List.of(TASK_STATUS_IN_PROGRESS, TASK_STATUS_TODO));
            // employeeLoadParams.put("createdDateStart", startDate); // 可选：是否只统计此周期内创建的任务负载
            // employeeLoadParams.put("createdDateEnd", endDate);
            List<Map<String, Object>> rawEmployeeLoad = taskMapper.countTasksByAssigneeAndStatusGrouped(employeeLoadParams);
            Map<String, EmployeeLoadDto> employeeLoadMap = new HashMap<>();
            for (Map<String, Object> row : rawEmployeeLoad) {
                String assignee = (String) row.get("assignee_employee");
                String status = (String) row.get("task_status");
                Integer count = ((Number) row.get("task_count")).intValue();

                EmployeeLoadDto loadDto = employeeLoadMap.computeIfAbsent(assignee, k -> EmployeeLoadDto.builder().employeeName(assignee).inProgressTaskCount(0).todoTaskCount(0).build());
                if (TASK_STATUS_IN_PROGRESS.equals(status)) {
                    loadDto.setInProgressTaskCount(loadDto.getInProgressTaskCount() + count);
                } else if (TASK_STATUS_TODO.equals(status)) {
                    loadDto.setTodoTaskCount(loadDto.getTodoTaskCount() + count);
                }
            }
            employeeLoadChart.addAll(employeeLoadMap.values());


            // 4. 项目类型分布
            Map<String, Object> projectTypeParams = new HashMap<>();
            projectTypeParams.put("groupByField", projectTypeDimension); // "businessTypeName" or "profitCenterZone"
            projectTypeParams.put("startDate", startDate);
            projectTypeParams.put("endDate", endDate);
            List<Map<String, Object>> rawProjectTypeDist = projectMapper.countProjectsGroupedByField(projectTypeParams);
            for (Map<String, Object> row : rawProjectTypeDist) {
                projectTypeDistributionChart.add(ChartDataPointDto.builder()
                        .label((String) row.get("label"))
                        .value(((Number) row.get("value")))
                        .build());
            }

        } catch (Exception e) {
            logger.error("获取部门整体概览数据失败: dateRange={}, dimension={}", dateRangeString, projectTypeDimension, e);
            // 可以选择返回部分数据或空数据，或者抛出自定义异常
        }

        return DepartmentOverviewStatsDto.builder()
                .kpis(kpis)
                .projectStatusDistribution(projectStatusDistributionChart)
                .employeeLoad(employeeLoadChart)
                .projectTypeDistribution(projectTypeDistributionChart)
                .build();
    }
    /**
     * 辅助方法：计算活跃项目数。
     * 活跃项目定义：在指定日期范围（基于项目创建日期）内，并且当前并非“已完成”或“已取消”状态的项目。
     * 由于项目状态是动态计算的，这里简化为：统计在指定日期范围内创建的，
     * 并且其下至少有一个任务的状态不是“已完成”或“已取消”的项目数量。
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 活跃项目数
     */
    private Integer calculateActiveProjectsCount(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> params = new HashMap<>();
        // params.put("activeStatusCodes", List.of("预期非最终状态")); // 如果 projectMapper 支持直接按项目状态筛选
        // 实际中，我们可能需要获取所有在日期范围内创建的项目
        // 然后对每个项目检查其任务状态来判断项目是否活跃
        List<Project> projectsInDateRange = projectMapper.findProjectsForStatistics(Map.of("createdDateStart",startDate, "createdDateEnd", endDate)); // 假设findAll支持日期范围
        int activeCount = 0;
        if (projectsInDateRange == null) return 0;

        for (Project project : projectsInDateRange) {
            Map<String, Object> taskParams = new HashMap<>();
            taskParams.put("projectId", project.getProjectId());
            taskParams.put("statusList", List.of(TASK_STATUS_TODO, TASK_STATUS_IN_PROGRESS, TASK_STATUS_PAUSED)); // 假设这些是活跃任务状态
            Integer activeTasksInProject = taskMapper.countTasksByCriteria(taskParams);
            if (activeTasksInProject > 0) {
                activeCount++;
            }
        }
        return activeCount;
    }


    @Override
    public List<AtRiskProjectDto> getAtRiskProjects() {
        List<AtRiskProjectDto> atRiskProjectDtos = new ArrayList<>();
        try {
            // 1. 获取所有“活跃”项目 (非“已完成”或“已取消”)
            // 假设 selectActiveProjectsForRiskAssessment 返回的是这类项目的基础信息
            // 如果 project_db_status 不存在，则需要获取所有项目，然后根据任务判断活跃性
            Map<String, Object> projectParams = new HashMap<>();
            // projectParams.put("activeStatusCodes", List.of("进行中", "待办", "已暂停")); // 理想情况
            List<Project> activeProjects = projectMapper.selectActiveProjectsForRiskAssessment(projectParams);
            if (activeProjects == null) return Collections.emptyList();


            for (Project project : activeProjects) {
                Map<String, Object> taskParams = new HashMap<>();
                taskParams.put("projectId", project.getProjectId());

                // 2. 统计逾期任务数
                taskParams.put("isOverdue", true);
                Integer overdueTaskCount = taskMapper.countTasksByCriteria(taskParams);

                // 3. 统计未来3天到期的高优未开始任务
                taskParams.clear();
                taskParams.put("projectId", project.getProjectId());
                taskParams.put("dueDateStart", LocalDate.now());
                taskParams.put("dueDateEnd", LocalDate.now().plusDays(2)); // 今天, 明天, 后天
                taskParams.put("priorityList", List.of(TASK_PRIORITY_HIGH));
                taskParams.put("statusList", List.of(TASK_STATUS_TODO));
                List<Task> upcomingHighPriorityTasks = taskMapper.selectTasksForStatistics(taskParams);
                int numUpcomingHighPriorityNotStarted = upcomingHighPriorityTasks.size();

                // 4. 判断风险等级和生成描述
                String riskLevel = RISK_LEVEL_NONE;
                StringBuilder riskDescriptionBuilder = new StringBuilder();

                if (overdueTaskCount > 0) {
                    riskDescriptionBuilder.append("存在").append(overdueTaskCount).append("个逾期任务。");
                }
                if (numUpcomingHighPriorityNotStarted > 0) {
                    riskDescriptionBuilder.append("未来3天内有").append(numUpcomingHighPriorityNotStarted)
                            .append("个高优先级任务（如：")
                            .append(upcomingHighPriorityTasks.stream().map(Task::getTaskName).limit(1).collect(Collectors.joining()))
                            .append(numUpcomingHighPriorityNotStarted > 1 ? "等" : "")
                            .append("）即将到期且未开始。");
                }

                if (overdueTaskCount >= 3 || (overdueTaskCount >= 1 && numUpcomingHighPriorityNotStarted >= 1)) {
                    riskLevel = RISK_LEVEL_HIGH;
                } else if (overdueTaskCount > 0 || numUpcomingHighPriorityNotStarted > 0) {
                    riskLevel = RISK_LEVEL_MEDIUM;
                }
                // (可选) 低风险定义：如果需要，可以添加更细致的低风险判断逻辑

                if (!RISK_LEVEL_NONE.equals(riskLevel)) {
                    atRiskProjectDtos.add(AtRiskProjectDto.builder()
                            .projectId(project.getProjectId())
                            .projectName(project.getProjectName())
                            .projectManager(project.getProjectManagerEmployee())
                            .riskLevel(riskLevel)
                            .overdueTaskCount(overdueTaskCount)
                            .riskDescription(riskDescriptionBuilder.length() > 0 ? riskDescriptionBuilder.toString() : "暂无明确风险点。")
                            .build());
                }
            }
        } catch (Exception e) {
            logger.error("获取风险项目列表失败", e);
        }
        return atRiskProjectDtos;
    }

    @Override
    public TaskDeadlineStatsDto getTaskDeadlineInfo(int upcomingDueDays, boolean useGlobalOverdue) {
        List<TaskDeadlineItemDto> upcomingTaskDtos = new ArrayList<>();
        List<TaskDeadlineItemDto> overdueTaskDtos = new ArrayList<>();
        LocalDate today = LocalDate.now();

        try {
            // 1. 获取即将到期的任务
            Map<String, Object> upcomingParams = new HashMap<>();
            upcomingParams.put("dueDateStart", today);
            upcomingParams.put("dueDateEnd", today.plusDays(upcomingDueDays - 1)); // 例如7天，则今天到今天+6天
            upcomingParams.put("statusList", List.of(TASK_STATUS_TODO, TASK_STATUS_IN_PROGRESS, TASK_STATUS_PAUSED)); // 非最终状态
            List<Task> upcomingTasks = taskMapper.selectTasksForStatistics(upcomingParams);
            if (upcomingTasks != null) {
                for (Task task : upcomingTasks) {
                    Project project = projectMapper.findById(task.getProjectId()); // 获取项目名称
                    upcomingTaskDtos.add(TaskDeadlineItemDto.builder()
                            .taskId(task.getTaskId())
                            .taskName(task.getTaskName())
                            .projectId(task.getProjectId())
                            .projectName(project != null ? project.getProjectName() : "N/A")
                            .assignee(task.getAssigneeEmployee())
                            .dueDate(task.getDueDate())
                            .priority(task.getPriority())
                            .build());
                }
            }

            // 2. 获取已逾期的任务
            Map<String, Object> overdueParams = new HashMap<>();
            overdueParams.put("isOverdue", true);
            // useGlobalOverdue 开关: 如果为false，可能需要根据系统配置的默认周期来筛选逾期任务。
            // 当前实现：如果 true，则获取所有逾期；如果 false，则不应用额外日期过滤器（依赖isOverdue本身）。
            // 若要实现“仅本周期内产生的逾期”，则需传入 dateRange 并应用到任务创建/截止日期。
            // 鉴于API设计，此处简化为：useGlobalOverdue 主要控制是否对逾期任务应用更广泛的筛选。
            // 目前，它不直接改变查询逻辑，因为 isOverdue=true 本身就是全局的。
            // 如果需要更细致的控制，比如只看“最近N天内变成逾期的任务”，SQL会更复杂。
            List<Task> overdueTasks = taskMapper.selectTasksForStatistics(overdueParams);
            if (overdueTasks != null) {
                for (Task task : overdueTasks) {
                    Project project = projectMapper.findById(task.getProjectId());
                    long daysOverdue = ChronoUnit.DAYS.between(task.getDueDate(), today);
                    overdueTaskDtos.add(TaskDeadlineItemDto.builder()
                            .taskId(task.getTaskId())
                            .taskName(task.getTaskName())
                            .projectId(task.getProjectId())
                            .projectName(project != null ? project.getProjectName() : "N/A")
                            .assignee(task.getAssigneeEmployee())
                            .dueDate(task.getDueDate())
                            .priority(task.getPriority())
                            .overdueDays((int) daysOverdue)
                            .build());
                }
            }
        } catch (Exception e) {
            logger.error("获取任务到期与逾期信息失败: upcomingDueDays={}", upcomingDueDays, e);
        }

        return TaskDeadlineStatsDto.builder()
                .upcomingTasks(upcomingTaskDtos)
                .overdueTasks(overdueTaskDtos)
                .build();
    }

    @Override
    public EmployeeWorkDetailsDto getEmployeeWorkDetails(String employeeName, String dateRangeString) {
        DateUtils.DateRange parsedDateRange = dateUtils.parseDateRange(dateRangeString);
        LocalDate startDate = parsedDateRange.getStartDate();
        LocalDate endDate = parsedDateRange.getEndDate();

        KpiDto kpis = new KpiDto();
        List<EmployeeProjectTaskDto> tasksByProject = new ArrayList<>();

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("assigneeEmployee", employeeName);
            // params.put("createdDateStart", startDate); // 根据KPI定义是否受dateRange影响
            // params.put("createdDateEnd", endDate);

            params.put("statusList", List.of(TASK_STATUS_TODO));
            kpis.setEmployeeTodoTasks(taskMapper.countTasksByCriteria(params));

            params.put("statusList", List.of(TASK_STATUS_IN_PROGRESS));
            kpis.setEmployeeInProgressTasks(taskMapper.countTasksByCriteria(params));

            params.clear(); // 清空statusList，只用isOverdue
            params.put("assigneeEmployee", employeeName);
            params.put("isOverdue", true);
            // params.put("createdDateStart", startDate); // 逾期任务是否也受dateRange影响
            // params.put("createdDateEnd", endDate);
            kpis.setEmployeeOverdueTasks(taskMapper.countTasksByCriteria(params));

            // 获取员工所有相关任务 (不过滤状态，以便在前端展示所有类型的任务)
            params.clear();
            params.put("assigneeEmployee", employeeName);
            params.put("createdDateStart", startDate); // 任务列表通常受时间范围影响
            params.put("createdDateEnd", endDate);
            List<Task> employeeTasks = taskMapper.selectTasksForStatistics(params);

            if (employeeTasks != null) {
                Map<Long, List<EmployeeTaskSummaryDto>> tasksGroupedByProjectId = new HashMap<>();
                Map<Long, String> projectNamesMap = new HashMap<>(); // 缓存项目名称

                for (Task task : employeeTasks) {
                    tasksGroupedByProjectId
                            .computeIfAbsent(task.getProjectId(), k -> new ArrayList<>())
                            .add(EmployeeTaskSummaryDto.builder()
                                    .taskId(task.getTaskId())
                                    .taskName(task.getTaskName())
                                    .status(task.getTaskStatus())
                                    .dueDate(task.getDueDate())
                                    .priority(task.getPriority())
                                    .isOverdue(task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now()) && !TASK_STATUS_COMPLETED.equals(task.getTaskStatus()))
                                    .build());
                    if (!projectNamesMap.containsKey(task.getProjectId())) {
                        Project project = projectMapper.findById(task.getProjectId());
                        projectNamesMap.put(task.getProjectId(), project != null ? project.getProjectName() : "未知项目");
                    }
                }

                for (Map.Entry<Long, List<EmployeeTaskSummaryDto>> entry : tasksGroupedByProjectId.entrySet()) {
                    tasksByProject.add(EmployeeProjectTaskDto.builder()
                            .projectId(entry.getKey())
                            .projectName(projectNamesMap.get(entry.getKey()))
                            .tasks(entry.getValue())
                            .build());
                }
            }

        } catch (Exception e) {
            logger.error("获取员工 {} 的工作详情失败: dateRange={}", employeeName, dateRangeString, e);
        }

        return EmployeeWorkDetailsDto.builder()
                .kpis(kpis)
                .tasksByProject(tasksByProject)
                .build();
    }
}
