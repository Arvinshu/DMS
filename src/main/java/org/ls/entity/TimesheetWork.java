package org.ls.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 实体类：工时主表 (t_wkt)
 * 注意：此类对应的数据来源于业务系统，表结构不可更改。
 * 文件路径: src/main/java/org/ls/entity/TimesheetWork.java
 */
@Data // Lombok 注解：自动生成 getter, setter, toString, equals, hashCode 方法
@NoArgsConstructor // Lombok 注解：自动生成无参构造函数
@AllArgsConstructor // Lombok 注解：自动生成全参构造函数
public class TimesheetWork implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工时申请单号 (ts_id)
     * 复合主键之一
     */
    private String tsId;

    /**
     * 工时区间 (tr)
     */
    private String tr;

    /**
     * 员工信息，工号+姓名组合 (employee)
     * 复合主键之一
     * 例如：E1001-张三
     */
    private String employee;

    /**
     * 部门名称，完整部门名称 (dep)
     * 例如：华东研发中心-大数据部
     */
    private String dep;

    /**
     * 实际填报工时部门，完整部门名称 (ts_Dep)
     * 例如：华东研发中心-大数据部
     */
    private String tsDep;

    /**
     * 申请状态 (ts_status)
     */
    private String tsStatus;

    /**
     * 财务年月 (ts_ym)
     * 例如：2025-03
     */
    private String tsYm;

    /**
     * 自然年月 (nature_ym)
     * 例如：2025-03
     */
    private String natureYm;

    /**
     * 工时日期 (ts_date)
     * 复合主键之一
     */
    private LocalDate tsDate;

    /**
     * 工时小时数 (ts_hours)
     */
    private Float tsHours;

    /**
     * 人月数 (ts_month)
     */
    private Float tsMonth;

    /**
     * 项目编码 (proj_bm)
     */
    private String projBm;

    /**
     * 工时编码 (ts_bm)
     */
    private String tsBm;

    /**
     * 工时名称 (ts_name)
     */
    private String tsName;

    /**
     * 利润中心全名 (zone)
     * 示例："基础业务-大区与网省中心-大区四-冀北中心-业务-大营销咨询业务部"
     */
    private String zone;

    /**
     * 支持工时编码 (s_proj_bm) - 注意数据库字段名是 s_proj_bm，不是 s_ts_bm
     */
    private String sProjBm;

    /**
     * 支持工时名称 (s_ts_bm) - 注意数据库字段名是 s_ts_bm，不是 s_ts_name
     */
    private String sTsBm;

    /**
     * 工时备注 (ts_comments)
     */
    private String tsComments;

    // 注意：由于使用了 Lombok @Data, getter/setter 等方法会自动生成，无需手动编写。
    // 如果不使用 Lombok，需要手动添加所有字段的 getter 和 setter 方法，以及构造函数和 toString 等。
    // 例如:
    // public String getTsId() { return tsId; }
    // public void setTsId(String tsId) { this.tsId = tsId; }
    // ... 其他所有字段
}
