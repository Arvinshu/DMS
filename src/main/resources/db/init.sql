-- 文件路径: src/main/resources/db/init.sql
-- 删除已存在的表（如果需要重新初始化）
DROP TABLE IF EXISTS public.t_wkt CASCADE;
DROP TABLE IF EXISTS public.t_department CASCADE;
DROP TABLE IF EXISTS public.t_employee CASCADE;
DROP TABLE IF EXISTS public.t_timesheet_code CASCADE;
DROP TABLE IF EXISTS public.t_business_type CASCADE;
DROP TABLE IF EXISTS public.t_profit_center CASCADE;

-- 1. 工时主表 (t_wkt) - 结构按要求提供，不可更改
CREATE TABLE IF NOT EXISTS public.t_wkt
(
    ts_id         VARCHAR(30) NOT NULL,        -- 工时申请单号
    tr            VARCHAR(50) NOT NULL,        -- 工时区间
    employee      VARCHAR(30) NOT NULL,        -- 员工信息 (工号+姓名)
    dep           VARCHAR(200) NOT NULL,       -- 部门名称
    ts_dep        VARCHAR(200) NOT NULL,       -- 填写工时的时候所在部门名称
    ts_status     VARCHAR(30) NOT NULL,        -- 申请状态
    ts_ym         VARCHAR(10) NOT NULL,        -- 财务年月
    nature_ym     VARCHAR(10) NOT NULL,        -- 自然年月
    ts_date       DATE NOT NULL,               -- 工时日期
    ts_hours      REAL NOT NULL,            -- 工时小时数
    ts_month      REAL NOT NULL,               -- 人月数
    proj_bm       VARCHAR(30),                 -- 项目编码
    ts_bm         VARCHAR(200),                -- 工时编码
    ts_name       VARCHAR(200),                -- 工时名称
    zone          VARCHAR(200),                -- 利润中心
    s_proj_bm     VARCHAR(30),                 -- 支持工时编码
    s_ts_bm       VARCHAR(200),                -- 支持工时名称
    ts_comments   VARCHAR(2000),               -- 工时备注
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 记录创建时间
    -- 添加复合主键 (根据需求)
    PRIMARY KEY (ts_id, employee, ts_date, ts_bm)
);

-- 表注释
COMMENT ON TABLE public.t_wkt IS '工时主表 (数据来源于业务系统)';
-- 列注释
COMMENT ON COLUMN public.t_wkt.ts_id IS '工时申请单号';
COMMENT ON COLUMN public.t_wkt.tr IS '工时区间';
COMMENT ON COLUMN public.t_wkt.employee IS '员工信息，工号+姓名组合（如：E1001-张三）';
COMMENT ON COLUMN public.t_wkt.dep IS '部门名称，完整部门名称（如：华东研发中心-大数据部）';
COMMENT ON COLUMN public.t_wkt.ts_dep IS '填写工时的时候所在部门名称，完整部门名称（如：华东研发中心-大数据部）';
COMMENT ON COLUMN public.t_wkt.ts_status IS '申请状态';
COMMENT ON COLUMN public.t_wkt.ts_ym IS '财务年月';
COMMENT ON COLUMN public.t_wkt.nature_ym IS '自然年月';
COMMENT ON COLUMN public.t_wkt.ts_date IS '工时日期';
COMMENT ON COLUMN public.t_wkt.ts_hours IS '工时小时数';
COMMENT ON COLUMN public.t_wkt.ts_month IS '人月数';
COMMENT ON COLUMN public.t_wkt.proj_bm IS '项目编码';
COMMENT ON COLUMN public.t_wkt.ts_bm IS '工时编码';
COMMENT ON COLUMN public.t_wkt.ts_name IS '工时名称';
COMMENT ON COLUMN public.t_wkt.zone IS '利润中心全名（示例："基础业务-大区与网省中心-大区四-冀北中心-业务-大营销咨询业务部"）';
COMMENT ON COLUMN public.t_wkt.s_proj_bm IS '支持工时编码';
COMMENT ON COLUMN public.t_wkt.s_ts_bm IS '支持工时名称';
COMMENT ON COLUMN public.t_wkt.ts_comments IS '工时备注';
COMMENT ON COLUMN public.t_wkt.created_at IS '创建时间';

-- 设置表所有者
ALTER TABLE public.t_wkt OWNER TO postgres;


-- 2. 部门表 (t_department)
CREATE TABLE IF NOT EXISTS public.t_department
(
    id                   INTEGER PRIMARY KEY,                 -- 部门唯一标识，需手工维护的数字编号
    dep_name             VARCHAR(200) UNIQUE NOT NULL,        -- 部门名称 (从t_wkt同步, 页面只展示)
    dep_level            VARCHAR(50) NOT NULL,                -- 部门层级：一级部门/二级部门/三级部门
    manager_id           VARCHAR(30),                         -- 部门负责人ID (外键关联 t_employee.employee)
    assistant_manager_id VARCHAR(30),                         -- 部门副经理ID (外键关联 t_employee.employee)
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 记录创建时间
    updated_at           TIMESTAMP,                         -- 最后更新时间 (需应用程序或触发器维护)
    active               BOOLEAN DEFAULT TRUE NOT NULL,       -- 启用状态：true-启用，false-停用
    is_statistics        BOOLEAN DEFAULT TRUE NOT NULL        -- 是否参与统计：true-参与，false-不参与
);

-- 表注释
COMMENT ON TABLE public.t_department IS '部门信息表';
-- 列注释
COMMENT ON COLUMN public.t_department.id IS '部门唯一标识，需手工维护的数字编号';
COMMENT ON COLUMN public.t_department.dep_name IS '部门名称，对t_wkt.dep进行去重后同步至该列。此列数据是从其他表同步过来，在页面只展示不维护';
COMMENT ON COLUMN public.t_department.dep_level IS '部门层级：一级部门/二级部门/三级部门';
COMMENT ON COLUMN public.t_department.manager_id IS '部门负责人ID（关联t_employee.employee）';
COMMENT ON COLUMN public.t_department.assistant_manager_id IS '部门副经理ID（关联t_employee.employee）';
COMMENT ON COLUMN public.t_department.created_at IS '记录创建时间（自动生成）';
COMMENT ON COLUMN public.t_department.updated_at IS '最后更新时间（需应用程序或触发器维护）';
COMMENT ON COLUMN public.t_department.active IS '启用状态：true-启用，false-停用';
COMMENT ON COLUMN public.t_department.is_statistics IS '是否参与统计：true-参与，false-不参与';

-- 设置表所有者
ALTER TABLE public.t_department OWNER TO postgres;

-- 3. 员工表 (t_employee)
CREATE TABLE IF NOT EXISTS public.t_employee
(
    employee      VARCHAR(30) PRIMARY KEY,             -- 员工信息 (工号+姓名, 主键, 从t_wkt同步, 页面只展示)
    employee_id   VARCHAR(20) NOT NULL UNIQUE,         -- 员工唯一工号 (从employee拆分, 页面只展示)
    employee_name VARCHAR(50) NOT NULL,                -- 员工真实姓名 (从employee拆分, 页面只展示)
    dep_id        INTEGER,                             -- 所属部门ID (外键关联 t_department.id)
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 记录创建时间
    updated_at    TIMESTAMP,                         -- 最后更新时间 (需应用程序或触发器维护)
    active        BOOLEAN DEFAULT TRUE NOT NULL,       -- 在职状态：true-在职，false-离职
    is_statistics BOOLEAN DEFAULT TRUE NOT NULL        -- 工时统计标识：true-参与统计，false-不参与
);

-- 表注释
COMMENT ON TABLE public.t_employee IS '员工信息表';
-- 列注释
COMMENT ON COLUMN public.t_employee.employee IS '员工信息，主键，对t_wkt.employee进行去重后同步至该列。此列数据是从其他表同步过来，在页面只展示不维护';
COMMENT ON COLUMN public.t_employee.employee_id IS '员工唯一工号,从t_wkt.employee按照"-"号拆分后第1个数据。此列数据是从其他表同步过来，在页面只展示不维护';
COMMENT ON COLUMN public.t_employee.employee_name IS '员工真实姓名,从t_wkt.employee按照"-"号拆分后第2个数据。此列数据是从其他表同步过来，在页面只展示不维护';
COMMENT ON COLUMN public.t_employee.dep_id IS '所属部门ID（关联t_department.id）';
COMMENT ON COLUMN public.t_employee.created_at IS '记录创建时间（自动生成）';
COMMENT ON COLUMN public.t_employee.updated_at IS '最后更新时间（需应用程序或触发器维护）';
COMMENT ON COLUMN public.t_employee.active IS '在职状态：true-在职，false-离职';
COMMENT ON COLUMN public.t_employee.is_statistics IS '工时统计标识：true-参与统计，false-不参与';

-- 设置表所有者
ALTER TABLE public.t_employee OWNER TO postgres;

-- 添加外键约束 (员工 -> 部门)
ALTER TABLE public.t_employee
    ADD CONSTRAINT fk_employee_department
        FOREIGN KEY (dep_id) REFERENCES public.t_department(id);

-- 添加外键约束 (部门 -> 员工)
ALTER TABLE public.t_department
    ADD CONSTRAINT fk_department_manager
        FOREIGN KEY (manager_id) REFERENCES public.t_employee(employee);

ALTER TABLE public.t_department
    ADD CONSTRAINT fk_department_assistant_manager
        FOREIGN KEY (assistant_manager_id) REFERENCES public.t_employee(employee);


-- 4. 业务类型表 (t_business_type)
CREATE TABLE IF NOT EXISTS public.t_business_type
(
    id                   SERIAL PRIMARY KEY,                  -- 自增主键
    business_category    VARCHAR(200) NOT NULL,               -- 业务类别
    business_name        VARCHAR(200) NOT NULL UNIQUE,        -- 业务名称 (需要唯一，用于关联)
    business_description VARCHAR(200),                        -- 业务内容描述
    is_enabled           BOOLEAN DEFAULT TRUE NOT NULL,       -- 业务是否启用
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 数据创建时间
    updated_at           TIMESTAMP                          -- 数据更新时间
);

-- 表注释
COMMENT ON TABLE public.t_business_type IS '业务类型表';
-- 列注释
COMMENT ON COLUMN public.t_business_type.id IS '自增主键';
COMMENT ON COLUMN public.t_business_type.business_category IS '业务类别';
COMMENT ON COLUMN public.t_business_type.business_name IS '描述业务名称信息';
COMMENT ON COLUMN public.t_business_type.business_description IS '任务内容详细描述';
COMMENT ON COLUMN public.t_business_type.is_enabled IS '业务当前是否处于活动状态: true-启用, false-停用';
COMMENT ON COLUMN public.t_business_type.created_at IS '数据创建时间（自动生成）';
COMMENT ON COLUMN public.t_business_type.updated_at IS '数据更新时间（应用程序维护）';

-- 设置表所有者
ALTER TABLE public.t_business_type OWNER TO postgres;

-- 5. 工时编码表 (t_timesheet_code)
CREATE TABLE IF NOT EXISTS public.t_timesheet_code
(
    ts_bm                 VARCHAR(200) PRIMARY KEY,            -- 工时编码 (主键, 从t_wkt同步, 页面只展示)
    ts_name               VARCHAR(200),                        -- 工时名称 (从t_wkt同步, 页面只展示)
    s_ts_bm               VARCHAR(200),                        -- 子工时编码 (从t_wkt同步, 页面只展示)
    custom_project_name   VARCHAR(200),                        -- 工时信息 (部门维护的项目名称)
    is_project_timesheet  BOOLEAN DEFAULT TRUE NOT NULL,       -- 是否项目工时
    is_enabled            BOOLEAN DEFAULT TRUE NOT NULL,       -- 工时启用标识
    project_business_type VARCHAR(200),                        -- 项目业务类型 (外键关联 t_business_type.business_name)
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 数据创建时间
    updated_at            TIMESTAMP                          -- 数据更新时间
);

-- 表注释
COMMENT ON TABLE public.t_timesheet_code IS '工时编码维护表';
-- 列注释
COMMENT ON COLUMN public.t_timesheet_code.ts_bm IS '工时编码，从 t_wkt.ts_bm 去重后同步过来，此列数据是从其他表同步过来，在页面只展示不维护';
COMMENT ON COLUMN public.t_timesheet_code.ts_name IS '工时名称， 将 t_wkt.ts_bm 对应的t_wkt.ts_name 同步过来，此列数据是从其他表同步过来，在页面只展示不维护';
COMMENT ON COLUMN public.t_timesheet_code.s_ts_bm IS '子工时编码，将 t_wkt.ts_bm 对应的t_wkt.s_ts_bm 同步过来，此列数据是从其他表同步过来，在页面只展示不维护';
COMMENT ON COLUMN public.t_timesheet_code.custom_project_name IS '由于工时名称和实际项目名称不一致，此字段用以部门自己维护的项目名称';
COMMENT ON COLUMN public.t_timesheet_code.is_project_timesheet IS '用以标识项目工时和非项目工时，此字段还用以后期是否参与工时统计';
COMMENT ON COLUMN public.t_timesheet_code.is_enabled IS '该工时是否处于启用状态，true为启用，false为停用。';
COMMENT ON COLUMN public.t_timesheet_code.project_business_type IS '用以对项目按照业务进行分类，标记项目属于哪一类业务，和“业务类型表”的“业务名称”列进行联动。';
COMMENT ON COLUMN public.t_timesheet_code.created_at IS '数据创建时间（自动生成）';
COMMENT ON COLUMN public.t_timesheet_code.updated_at IS '数据更新时间（应用程序维护）';

-- 设置表所有者
ALTER TABLE public.t_timesheet_code OWNER TO postgres;

-- 添加外键约束 (工时编码 -> 业务类型)
ALTER TABLE public.t_timesheet_code
    ADD CONSTRAINT fk_timesheet_code_business_type
        FOREIGN KEY (project_business_type) REFERENCES public.t_business_type(business_name);

-- 6. 利润中心表 (t_profit_center)
CREATE TABLE IF NOT EXISTS public.t_profit_center
(
    zone                  VARCHAR(200) PRIMARY KEY,            -- 利润中心全名 (主键, 从t_wkt同步, 页面只展示)
    business_type         VARCHAR(200),                        -- 业务类型 (自动拆分, 页面只展示)
    region_category       VARCHAR(200),                        -- 区域分类 (自动拆分, 页面只展示)
    region_name           VARCHAR(200),                        -- 大区名称 (自动拆分, 页面只展示)
    center_name           VARCHAR(200),                        -- 中心名称 (自动拆分, 页面只展示)
    business_subcategory  VARCHAR(200),                        -- 业务子类 (自动拆分, 页面只展示)
    department_name       VARCHAR(200),                        -- 部门名称 (自动拆分, 页面只展示)
    responsible_person    VARCHAR(100),                        -- 区域负责人 (部门维护)
    work_location         VARCHAR(200),                        -- 主要工作地点 (部门维护)
    custom_zone_remark    VARCHAR(200),                        -- 利润中心备注 (部门维护)
    is_enabled            BOOLEAN DEFAULT TRUE NOT NULL,       -- 利润中心启用状态
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 数据创建时间
    updated_at            TIMESTAMP                          -- 数据更新时间
);

-- 表注释
COMMENT ON TABLE public.t_profit_center IS '利润中心维护表';
-- 列注释
COMMENT ON COLUMN public.t_profit_center.zone IS '利润中心全名，对t_wkt.zone去重后的数据写入该列,此列数据是从其他表同步过来，在页面只展示不维护';
COMMENT ON COLUMN public.t_profit_center.business_type IS '业务类型（示例："基础业务"）,自动将 t_wkt.zone 去重后的数据按"-"号拆分后填入第1个数据，在页面只展示不维护';
COMMENT ON COLUMN public.t_profit_center.region_category IS '区域分类（示例："大区与网省中心"）,自动将 t_wkt.zone 去重后的数据按"-"号拆分后填入第2个数据，在页面只展示不维护';
COMMENT ON COLUMN public.t_profit_center.region_name IS '大区名称（示例："大区四"）,自动将 t_wkt.zone 去重后的数据按"-"号拆分后填入第3个数据，在页面只展示不维护';
COMMENT ON COLUMN public.t_profit_center.center_name IS '中心名称（示例："冀北中心"），自动将 t_wkt.zone 去重后的数据按"-"号拆分后填入第4个数据，在页面只展示不维护';
COMMENT ON COLUMN public.t_profit_center.business_subcategory IS '业务子类（示例："业务"），自动将 t_wkt.zone 去重后的数据按"-"号拆分后填入第5个数据，在页面只展示不维护';
COMMENT ON COLUMN public.t_profit_center.department_name IS '部门名称（示例："大营销咨询业务部"）,自动将 t_wkt.zone 去重后的数据按"-"号拆分后填入第6个数据，在页面只展示不维护';
COMMENT ON COLUMN public.t_profit_center.responsible_person IS '区域负责人';
COMMENT ON COLUMN public.t_profit_center.work_location IS '主要工作地点';
COMMENT ON COLUMN public.t_profit_center.custom_zone_remark IS '利润中心备注，利润中心全名长度不一，需要手工将利润中心名称截取一部分，部门自己维护的利润中心名称';
COMMENT ON COLUMN public.t_profit_center.is_enabled IS '利润中心启用状态: true-启用, false-停用';
COMMENT ON COLUMN public.t_profit_center.created_at IS '数据创建时间（自动生成）';
COMMENT ON COLUMN public.t_profit_center.updated_at IS '数据更新时间（应用程序维护）';

-- 设置表所有者
ALTER TABLE public.t_profit_center OWNER TO postgres;

-- === 为 t_wkt 表添加索引 ===
-- 根据查询需求，为经常用于 WHERE 条件、JOIN 或 GROUP BY 的列创建索引
CREATE INDEX IF NOT EXISTS idx_t_wkt_ts_date ON public.t_wkt (ts_date);
COMMENT ON INDEX public.idx_t_wkt_ts_date IS '工时表按日期查询索引';

CREATE INDEX IF NOT EXISTS idx_t_wkt_employee ON public.t_wkt (employee);
COMMENT ON INDEX public.idx_t_wkt_employee IS '工时表按员工查询索引';

CREATE INDEX IF NOT EXISTS idx_t_wkt_ts_bm ON public.t_wkt (ts_bm);
COMMENT ON INDEX public.idx_t_wkt_ts_bm IS '工时表按工时编码查询索引';

CREATE INDEX IF NOT EXISTS idx_t_wkt_zone ON public.t_wkt (zone);
COMMENT ON INDEX public.idx_t_wkt_zone IS '工时表按利润中心查询索引';

CREATE INDEX IF NOT EXISTS idx_t_wkt_dep ON public.t_wkt (dep);
COMMENT ON INDEX public.idx_t_wkt_dep IS '工时表按部门名称查询索引';

-- 复合索引示例 (如果经常同时按这几个条件查询)
-- CREATE INDEX IF NOT EXISTS idx_t_wkt_emp_date_bm ON public.t_wkt (employee, ts_date, ts_bm);

-- === 为 t_employee 表添加索引 ===
CREATE INDEX IF NOT EXISTS idx_t_employee_dep_id ON public.t_employee (dep_id);
COMMENT ON INDEX public.idx_t_employee_dep_id IS '员工表按部门ID查询索引';

CREATE INDEX IF NOT EXISTS idx_t_employee_active_stats ON public.t_employee (active, is_statistics);
COMMENT ON INDEX public.idx_t_employee_active_stats IS '员工表按活动和统计状态查询索引';

-- === 为 t_department 表添加索引 ===
CREATE INDEX IF NOT EXISTS idx_t_department_active_stats ON public.t_department (active, is_statistics);
COMMENT ON INDEX public.idx_t_department_active_stats IS '部门表按活动和统计状态查询索引';

CREATE INDEX IF NOT EXISTS idx_t_department_dep_level ON public.t_department (dep_level);
COMMENT ON INDEX public.idx_t_department_dep_level IS '部门表按层级查询索引';

CREATE INDEX IF NOT EXISTS idx_t_department_dep_name ON public.t_department (dep_name);
COMMENT ON INDEX public.idx_t_department_dep_name IS '部门表按名称查询索引 (虽然有唯一约束，显式索引有时仍有帮助)';


-- === 为 t_timesheet_code 表添加索引 ===
CREATE INDEX IF NOT EXISTS idx_t_timesheet_code_is_project ON public.t_timesheet_code (is_project_timesheet);
COMMENT ON INDEX public.idx_t_timesheet_code_is_project IS '工时编码表按是否项目工时查询索引';

-- === 为 t_profit_center 表添加索引 ===
CREATE INDEX IF NOT EXISTS idx_t_profit_center_remark ON public.t_profit_center (custom_zone_remark);
COMMENT ON INDEX public.idx_t_profit_center_remark IS '利润中心表按自定义备注查询/分组索引';

CREATE INDEX IF NOT EXISTS idx_t_profit_center_sort ON public.t_profit_center (region_category, region_name, custom_zone_remark);
COMMENT ON INDEX public.idx_t_profit_center_sort IS '利润中心表按区域类别、名称、备注排序索引';

SELECT '数据库索引创建完毕 (如果不存在)';

-- 注意：对于 t_wkt 中 zone 为 null 的情况，不会插入到 t_profit_center 表中

-- 数据库维护：清空所有表数据
-- truncate table t_employee,t_department,t_timesheet_code,t_profit_center, t_business_type, t_timesheet_code;
-- truncate table t_wkt;

-- 数据库维护：删除所有表
-- drop  table t_employee,t_department,t_timesheet_code,t_profit_center, t_business_type, t_timesheet_code;
-- drop table t_wkt;

-- 初始化完成
SELECT '数据库初始化脚本执行完毕';
