/**
 * 文件路径: src/main/java/org/ls/dto/ProjectCreateDto.java
 * 开发时间: 2025-04-24
 * 作者: Gemini
 * 代码用途: 项目创建和更新请求的数据传输对象
 */
package org.ls.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ls.entity.Project; // 引入实体类方便转换

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectCreateDto {

    /**
     * 项目名称 (必须，且不能为空白)
     */
    @NotBlank(message = "项目名称不能为空")
    @Size(max = 255, message = "项目名称长度不能超过255个字符")
    private String projectName;

    /**
     * 项目描述 (可选)
     */
    private String projectDescription;

    /**
     * 关联的业务类型名称 (可选)
     */
    @Size(max = 200, message = "业务类型名称长度不能超过200个字符")
    private String businessTypeName;

    /**
     * 关联的利润中心区域代码 (可选)
     */
    @Size(max = 200, message = "利润中心区域代码长度不能超过200个字符")
    private String profitCenterZone;

    /**
     * 关联的项目负责人员工信息 (工号+姓名) (可选)
     */
    @Size(max = 30, message = "负责人信息长度不能超过30个字符")
    private String projectManagerEmployee;

    /**
     * 关联的工时代码 (可选)
     */
    @Size(max = 200, message = "工时代码长度不能超过200个字符")
    private String tsBm;

    /**
     * 关联的标签 ID 列表 (可选)
     */
    private List<Long> tagIds = new ArrayList<>(); // 初始化为空列表，避免 NullPointerException

    /**
     * 创建人 (通常从当前登录用户获取，不在请求体中传递)
     * private String createdBy;
     */

    /**
     * 更新人 (通常从当前登录用户获取，不在请求体中传递)
     * private String updatedBy;
     */


    /**
     * 将 DTO 对象转换为实体对象 (用于创建或更新)
     * 注意：不包含关联的 tags，也不包含时间戳和创建/更新人信息，这些由 Service 层处理
     *
     * @return Project 实体对象
     */
    public Project toEntity() {
        return Project.builder()
                // projectId 不在此设置，由 Service 层或数据库生成/指定
                .projectName(this.projectName)
                .projectDescription(this.projectDescription)
                .businessTypeName(this.businessTypeName)
                .profitCenterZone(this.profitCenterZone)
                .projectManagerEmployee(this.projectManagerEmployee)
                .tsBm(this.tsBm)
                // createdBy, updatedBy, createdAt, updatedAt 由 Service 层设置
                .build();
    }
}
