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





-- 插入 t_wkt 测试数据
INSERT INTO public.t_wkt (ts_id, tr, employee, dep, ts_status, ts_ym, nature_ym, ts_date, ts_hours, ts_month, proj_bm, ts_bm, ts_name, zone, s_proj_bm, s_ts_bm, ts_comments) VALUES
                                                                                                                                                                                  ('TS20250324654733', '第14.1周[2025-03-23]-[2025-03-25]', '35680-曹笑笑', '科技系统集成部集成设计部', '待提交', '2025-03', '2025-03', '2025-03-24', 8, 0.0476, 'LH18L001-S', 'LH18L001I-S', null, '基础业务-其他业务版块-BD业务-（支持子公司）-BD科技', 'BDKJ23H0359I1-SP', null, '处理14.1周工作：负责xxx项目方案交流参与xxx工作。'),
                                                                                                                                                                                  ('TS20250322640554', '第13周[2025-03-16]-[2025-03-22]', '10990-汤大敢', '科技系统集成部', '审批完', '2025-03', '2025-03', '2025-03-21', 8, 0.0476, 'LSKJ25R0092', 'LSKJ25R0092D', null, '基础业务-基础业务中台-能力中台-系统集成部', null, null, '处理13周工作：负责xxx项目方案交流参与xxx工作。'),
                                                                                                                                                                                  ('TS20250321635260', '第13周[2025-03-16]-[2025-03-22]', '31511-童长安', '科技系统集成部管理部', '审批中', '2025-03', '2025-03', '2025-03-17', 8, 0.0476, 'LSKJ25R0053', 'LSKJ25R0053F', null, '基础业务-基础业务中台-公司级研发-基础业务中台管理部', null, null, '处理13周工作：负责xxx项目方案交流参与xxx工作。'),
                                                                                                                                                                                  ('TS20250321636875', '第13周[2025-03-16]-[2025-03-22]', '33894-郑琳琳', '科技系统集成部运维部', '审批中', '2025-03', '2025-03', '2025-03-21', 8, 0.0476, 'LSKJ25X0145', 'LSKJ25X0145D', null, '基础业务-大区与网省中心-大区四-冀北中心-业务-大营销咨询业务部', null, null, '处理13周工作：负责xxx项目方案交流参与xxx工作。'),
                                                                                                                                                                                  ('TS20250228458763', '第10.2周[2025-02-26]-[2025-03-01]', '36693-陈安迪', '科技系统集成部集成交付部', '审批完', '2025-03', '2025-02', '2025-02-28', 4, 0.0263, null, '999005', null, null, 'LSKJ24H0935D-S', null, '处理10.2周工作：负责xxx项目方案交流参与xxx工作。'),
                                                                                                                                                                                  ('TS20250314578813', '第12周[2025-03-09]-[2025-03-15]', '36825-陈爱星', '科技系统集成部集成交付部', '审批完', '2025-03', '2025-03', '2025-03-10', 8, 0.0476, null, '999004', null, null, null, null, '处理12周工作：负责xxx项目方案交流参与xxx工作。'),
                                                                                                                                                                                  ('TS20250314570251', '第12周[2025-03-09]-[2025-03-15]', '36181-李小露', '科技系统集成部集成交付部', '审批完', '2025-03', '2025-03', '2025-03-14', 8, 0.0476, 'LH18L004-S', 'LH18L004I-S', null, '基础业务-其他业务版块-XY业务-（支持子公司）-XY科技', 'JXXY25X0014I', null, '处理12周工作：负责xxx项目方案交流参与xxx工作。'),
                                                                                                                                                                                  ('TS20250307521480', '第11周[2025-03-02]-[2025-03-08]', '36245-舒畅', '科技系统集成部集成设计部', '审批完', '2025-03', '2025-03', '2025-03-06', 4, 0.0238, 'LSKJ25X0145', 'LSKJ25X0145F', null, '基础业务-大区与网省中心-大区四-冀北中心-业务-大营销咨询业务部', null, null, '处理11周工作：负责xxx项目方案交流参与xxx工作。'),
                                                                                                                                                                                  ('TS20250324651077', '第13周[2025-03-16]-[2025-03-22]', '36245-舒畅', '科技系统集成部集成设计部', '审批中', '2025-03', '2025-03', '2025-03-17', 4, 0.0238, 'LSKJ25X0073', 'LSKJ25X0073P', null, '基础业务-大区与网省中心-大区三-北京中心-业务', null, null, '处理13周工作：负责xxx项目方案交流参与xxx工作。'),
                                                                                                                                                                                  ('TS20250321636968', '第13周[2025-03-16]-[2025-03-22]', '36272-刘涛', '科技系统集成部运维部', '审批完', '2025-03', '2025-03', '2025-03-21', 8, 0.0476, null, '999003', null, null, null, null, '处理13周工作：负责xxx项目方案交流参与xxx工作。');


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

-- 插入 t_department 测试数据 (dep_name 需要与 t_wkt 中的数据对应)
-- 注意：manager_id 和 assistant_manager_id 需要在 t_employee 插入后才能设置外键，或者先插入部门再更新员工信息
INSERT INTO public.t_department (id, dep_name, dep_level, manager_id, assistant_manager_id, active, is_statistics) VALUES
                                                                                                                       (100, '科技系统集成部', '一级部门', NULL, NULL, TRUE, TRUE),
                                                                                                                       (101, '科技系统集成部集成设计部', '二级部门', NULL, NULL, TRUE, TRUE),
                                                                                                                       (102, '科技系统集成部管理部', '二级部门', NULL, NULL, TRUE, TRUE),
                                                                                                                       (103, '科技系统集成部运维部', '二级部门', NULL, NULL, TRUE, TRUE),
                                                                                                                       (104, '科技系统集成部集成交付部', '二级部门', NULL, NULL, TRUE, TRUE);


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

-- 插入 t_employee 测试数据 (employee 需要与 t_wkt 中的数据对应)
-- 注意：dep_id 需要与 t_department 中的 id 对应
INSERT INTO public.t_employee (employee, employee_id, employee_name, dep_id, active, is_statistics) VALUES
                                                                                                        ('35680-曹笑笑', '35680', '曹笑笑', 101, TRUE, TRUE),
                                                                                                        ('10990-汤大敢', '10990', '汤大敢', 100, TRUE, TRUE),
                                                                                                        ('31511-童长安', '31511', '童长安', 102, TRUE, TRUE),
                                                                                                        ('33894-郑琳琳', '33894', '郑琳琳', 103, TRUE, TRUE),
                                                                                                        ('36693-陈安迪', '36693', '陈安迪', 104, TRUE, TRUE),
                                                                                                        ('36825-陈爱星', '36825', '陈爱星', 104, TRUE, TRUE),
                                                                                                        ('36181-李小露', '36181', '李小露', 104, TRUE, FALSE), -- 测试不参与统计
                                                                                                        ('36245-舒畅', '36245', '舒畅', 101, TRUE, TRUE),
                                                                                                        ('36272-刘涛', '36272', '刘涛', 103, FALSE, TRUE); -- 测试离职

-- 更新部门负责人信息 (示例)
UPDATE public.t_department SET manager_id = '10990-汤大敢' WHERE id = 100;
UPDATE public.t_department SET manager_id = '35680-曹笑笑', assistant_manager_id = '36245-舒畅' WHERE id = 101;


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

-- 插入 t_business_type 测试数据
INSERT INTO public.t_business_type (business_category, business_name, business_description, is_enabled) VALUES
                                                                                                            ('研发类', '核心产品研发', '公司核心软件产品的设计与开发', TRUE),
                                                                                                            ('实施类', '项目实施与交付', '客户现场项目部署、调试与交付', TRUE),
                                                                                                            ('咨询类', '技术咨询服务', '提供专业技术解决方案咨询', TRUE),
                                                                                                            ('运维类', '系统运维支持', '负责系统日常运维和故障处理', TRUE),
                                                                                                            ('管理类', '内部管理支持', '部门内部行政、协调等管理工作', FALSE); -- 测试停用


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

-- 插入 t_timesheet_code 测试数据 (ts_bm 需要与 t_wkt 中的数据对应)
INSERT INTO public.t_timesheet_code (ts_bm, ts_name, s_ts_bm, custom_project_name, is_project_timesheet, is_enabled, project_business_type) VALUES
                                                                                                                                                ('LH18L001I-S', null, null, 'BD科技支持项目', TRUE, TRUE, '核心产品研发'),
                                                                                                                                                ('LSKJ25R0092D', null, null, '系统集成部能力中台项目', TRUE, TRUE, '核心产品研发'),
                                                                                                                                                ('LSKJ25R0053F', null, null, '基础业务中台管理项目', TRUE, TRUE, '内部管理支持'),
                                                                                                                                                ('LSKJ25X0145D', null, null, '冀北中心大营销咨询', TRUE, TRUE, '技术咨询服务'),
                                                                                                                                                ('999005', null, 'LSKJ24H0935D-S', '内部培训与学习', FALSE, TRUE, '内部管理支持'), -- 非项目工时
                                                                                                                                                ('999004', null, null, '部门建设活动', FALSE, TRUE, '系统运维支持'), -- 非项目工时
                                                                                                                                                ('LH18L004I-S', null, 'JXXY25X0014I', 'XY科技支持项目', TRUE, TRUE, '项目实施与交付'),
                                                                                                                                                ('LSKJ25X0145F', null, null, '冀北中心大营销咨询-方案', TRUE, TRUE, '技术咨询服务'),
                                                                                                                                                ('LSKJ25X0073P', null, null, '北京中心业务支持', TRUE, FALSE, '技术咨询服务'), -- 测试停用
                                                                                                                                                ('999003', null, null, '休假/请假', FALSE, TRUE, '内部管理支持'); -- 非项目工时


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

-- 插入 t_profit_center 测试数据 (zone 需要与 t_wkt 中的数据对应)
-- 注意: business_type 等拆分字段的值应根据 zone 的实际内容设置，这里仅为示例
INSERT INTO public.t_profit_center (zone, business_type, region_category, region_name, center_name, business_subcategory, department_name, responsible_person, work_location, custom_zone_remark, is_enabled) VALUES
                                                                                                                                                                                                                  ('基础业务-其他业务版块-BD业务-（支持子公司）-BD科技', '基础业务', '其他业务版块', 'BD业务', '（支持子公司）', 'BD科技', NULL, '张三', '北京', 'BD科技支持', TRUE),
                                                                                                                                                                                                                  ('基础业务-基础业务中台-能力中台-系统集成部', '基础业务', '基础业务中台', '能力中台', '系统集成部', NULL, NULL, '李四', '上海', '能力中台-系统集成', TRUE),
                                                                                                                                                                                                                  ('基础业务-基础业务中台-公司级研发-基础业务中台管理部', '基础业务', '基础业务中台', '公司级研发', '基础业务中台管理部', NULL, NULL, '王五', '广州', '中台管理', TRUE),
                                                                                                                                                                                                                  ('基础业务-大区与网省中心-大区四-冀北中心-业务-大营销咨询业务部', '基础业务', '大区与网省中心', '大区四', '冀北中心', '业务', '大营销咨询业务部', '赵六', '天津', '冀北大营销', TRUE),
                                                                                                                                                                                                                  ('基础业务-其他业务版块-XY业务-（支持子公司）-XY科技', '基础业务', '其他业务版块', 'XY业务', '（支持子公司）', 'XY科技', NULL, '孙七', '深圳', 'XY科技支持', FALSE), -- 测试停用
                                                                                                                                                                                                                  ('基础业务-大区与网省中心-大区三-北京中心-业务', '基础业务', '大区与网省中心', '大区三', '北京中心', '业务', NULL, '周八', '北京', '北京中心业务', TRUE);

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
COMMENT ON INDEX public.idx_t_department_dep_name IS '部门表按名称查询索引';


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

-- 添加 t_wkt 的外键约束 (如果需要强制关联，但源表数据可能不规范，需谨慎)
-- ALTER TABLE public.t_wkt ADD CONSTRAINT fk_wkt_employee FOREIGN KEY (employee) REFERENCES public.t_employee(employee);
-- ALTER TABLE public.t_wkt ADD CONSTRAINT fk_wkt_department FOREIGN KEY (dep) REFERENCES public.t_department(dep_name); -- 不推荐，dep_name 可能变更
-- ALTER TABLE public.t_wkt ADD CONSTRAINT fk_wkt_timesheet_code FOREIGN KEY (ts_bm) REFERENCES public.t_timesheet_code(ts_bm); -- 可能因 null 值失败
-- ALTER TABLE public.t_wkt ADD CONSTRAINT fk_wkt_profit_center FOREIGN KEY (zone) REFERENCES public.t_profit_center(zone); -- 可能因 null 值失败

-- 建议：在应用程序逻辑中处理关联关系，而不是强制数据库外键，特别是对于从外部系统同步的数据。

-- 创建更新时间戳的触发器函数 (可选, 也可以在Java代码中设置)
-- CREATE OR REPLACE FUNCTION update_updated_at_column()
--     RETURNS TRIGGER AS $$
-- BEGIN
--     NEW.updated_at = now();
--     RETURN NEW;
-- END;
-- $$ language 'plpgsql';

-- 为需要自动更新 updated_at 的表创建触发器 (示例)
-- CREATE TRIGGER update_department_updated_at BEFORE UPDATE ON public.t_department FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
-- CREATE TRIGGER update_employee_updated_at BEFORE UPDATE ON public.t_employee FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
-- CREATE TRIGGER update_business_type_updated_at BEFORE UPDATE ON public.t_business_type FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
-- CREATE TRIGGER update_timesheet_code_updated_at BEFORE UPDATE ON public.t_timesheet_code FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
-- CREATE TRIGGER update_profit_center_updated_at BEFORE UPDATE ON public.t_profit_center FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 清空所有表数据
-- truncate table t_employee,t_department,t_timesheet_code,t_profit_center, t_business_type, t_timesheet_code;
-- truncate table t_wkt;

-- 初始化完成
SELECT '数据库初始化脚本执行完毕';




-- 文件路径: src/main/resources/db/init.sql
-- 在文件末尾添加以下内容

-- =====================================================================
--                  项目管理与任务管理相关表结构
-- =====================================================================

-- ----------------------------
-- 1. 项目阶段表 (固定字典表)
-- ----------------------------
-- DROP TABLE IF EXISTS public.t_project_stage CASCADE;
CREATE TABLE IF NOT EXISTS public.t_project_stage (
                                                      stage_id INT PRIMARY KEY,
                                                      stage_name VARCHAR(50) UNIQUE NOT NULL,
                                                      display_order INT NOT NULL -- 用于界面排序
);
COMMENT ON TABLE public.t_project_stage IS '项目固定阶段表';
COMMENT ON COLUMN public.t_project_stage.stage_id IS '阶段ID (手动设置: 1-策划, 2-需求, 3-部署, 4-上线, 5-运维)';
COMMENT ON COLUMN public.t_project_stage.stage_name IS '阶段名称';
COMMENT ON COLUMN public.t_project_stage.display_order IS '显示顺序';

-- 插入固定的阶段数据
INSERT INTO public.t_project_stage (stage_id, stage_name, display_order) VALUES
                                                                             (1, '策划', 10),
                                                                             (2, '需求设计', 20),
                                                                             (3, '部署验证', 30),
                                                                             (4, '上线验收', 40),
                                                                             (5, '系统运维', 50)
ON CONFLICT (stage_id) DO NOTHING; -- 如果已存在则忽略

-- ----------------------------
-- 2. 项目标签表 (字典表)
-- ----------------------------
-- DROP TABLE IF EXISTS public.t_tag CASCADE;
CREATE TABLE IF NOT EXISTS public.t_tag (
                                            tag_id SERIAL PRIMARY KEY, -- 使用 SERIAL 实现自增主键
                                            tag_name VARCHAR(100) UNIQUE NOT NULL
);
COMMENT ON TABLE public.t_tag IS '项目标签表';
COMMENT ON COLUMN public.t_tag.tag_id IS '标签ID (自增)';
COMMENT ON COLUMN public.t_tag.tag_name IS '标签名称 (唯一)';

-- 添加标签表索引
CREATE INDEX IF NOT EXISTS idx_t_tag_name ON public.t_tag (tag_name);

-- 插入示例标签数据
INSERT INTO public.t_tag (tag_name) VALUES
                                        ('核心业务'), ('创新项目'), ('技术改造'), ('客户支持'), ('内部管理')
ON CONFLICT (tag_name) DO NOTHING;

-- ----------------------------
-- 3. 项目表
-- ----------------------------
-- DROP TABLE IF EXISTS public.t_project CASCADE;
CREATE TABLE IF NOT EXISTS public.t_project (
                                                project_id BIGSERIAL PRIMARY KEY, -- 使用 BIGSERIAL 实现自增主键 (适用于可能的大量项目)
                                                project_name VARCHAR(255) UNIQUE NOT NULL,
                                                owner_name VARCHAR(100), -- 项目负责人 (手动维护)
                                                current_stage_id INT, -- 项目当前所处阶段 (手动设置，外键关联)
                                                creation_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                                update_time TIMESTAMP WITH TIME ZONE, -- 可用于记录最后更新时间

                                                CONSTRAINT fk_project_current_stage FOREIGN KEY (current_stage_id) REFERENCES public.t_project_stage(stage_id) ON DELETE SET NULL ON UPDATE CASCADE -- 如果阶段被删除，项目阶段置为NULL
);
COMMENT ON TABLE public.t_project IS '项目信息表';
COMMENT ON COLUMN public.t_project.project_id IS '项目ID (自增)';
COMMENT ON COLUMN public.t_project.project_name IS '项目名称 (唯一)';
COMMENT ON COLUMN public.t_project.owner_name IS '项目负责人姓名 (手动维护)';
COMMENT ON COLUMN public.t_project.current_stage_id IS '项目当前所处阶段ID (关联 t_project_stage)';
COMMENT ON COLUMN public.t_project.creation_time IS '项目创建时间';
COMMENT ON COLUMN public.t_project.update_time IS '项目最后更新时间';

-- 添加项目表索引
CREATE INDEX IF NOT EXISTS idx_t_project_name ON public.t_project (project_name);
CREATE INDEX IF NOT EXISTS idx_t_project_current_stage ON public.t_project (current_stage_id);
CREATE INDEX IF NOT EXISTS idx_t_project_creation_time ON public.t_project (creation_time);

-- ----------------------------
-- 4. 任务表
-- ----------------------------
-- DROP TABLE IF EXISTS public.t_task CASCADE;
CREATE TABLE IF NOT EXISTS public.t_task (
                                             task_id BIGSERIAL PRIMARY KEY,
                                             project_id BIGINT NOT NULL,
                                             task_name VARCHAR(255) NOT NULL,
                                             description TEXT,
                                             status VARCHAR(20) DEFAULT '待办' CHECK (status IN ('待办', '进行中', '已完成', '已取消')), -- 任务状态
                                             priority VARCHAR(10) DEFAULT '中' CHECK (priority IN ('高', '中', '低')), -- 优先级
                                             assignee VARCHAR(100), -- 负责人 (关联 t_employee.employee 字符串)
                                             due_date DATE, -- 截止日期
                                             stage_id INT NOT NULL, -- 所属阶段 (外键关联)
                                             creation_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                             update_time TIMESTAMP WITH TIME ZONE,

                                             CONSTRAINT fk_task_project FOREIGN KEY (project_id) REFERENCES public.t_project(project_id) ON DELETE CASCADE ON UPDATE CASCADE, -- 项目删除时级联删除任务
                                             CONSTRAINT fk_task_stage FOREIGN KEY (stage_id) REFERENCES public.t_project_stage(stage_id) ON DELETE RESTRICT ON UPDATE CASCADE -- 不允许删除仍有任务关联的阶段
);
COMMENT ON TABLE public.t_task IS '任务信息表';
COMMENT ON COLUMN public.t_task.task_id IS '任务ID (自增)';
COMMENT ON COLUMN public.t_task.project_id IS '所属项目ID (外键)';
COMMENT ON COLUMN public.t_task.task_name IS '任务名称';
COMMENT ON COLUMN public.t_task.description IS '任务描述';
COMMENT ON COLUMN public.t_task.status IS '任务状态 (待办, 进行中, 已完成, 已取消)';
COMMENT ON COLUMN public.t_task.priority IS '优先级 (高, 中, 低)';
COMMENT ON COLUMN public.t_task.assignee IS '负责人 (关联 t_employee.employee)';
COMMENT ON COLUMN public.t_task.due_date IS '截止日期';
COMMENT ON COLUMN public.t_task.stage_id IS '所属阶段ID (关联 t_project_stage)';
COMMENT ON COLUMN public.t_task.creation_time IS '任务创建时间';
COMMENT ON COLUMN public.t_task.update_time IS '任务最后更新时间';

-- 添加任务表索引
CREATE INDEX IF NOT EXISTS idx_t_task_project_id ON public.t_task (project_id);
CREATE INDEX IF NOT EXISTS idx_t_task_stage_id ON public.t_task (stage_id);
CREATE INDEX IF NOT EXISTS idx_t_task_assignee ON public.t_task (assignee);
CREATE INDEX IF NOT EXISTS idx_t_task_status ON public.t_task (status);
CREATE INDEX IF NOT EXISTS idx_t_task_due_date ON public.t_task (due_date);

-- ----------------------------
-- 5. 附件表
-- ----------------------------
-- DROP TABLE IF EXISTS public.t_attachment CASCADE;
CREATE TABLE IF NOT EXISTS public.t_attachment (
                                                   attachment_id BIGSERIAL PRIMARY KEY,
                                                   task_id BIGINT NOT NULL,
                                                   file_name VARCHAR(255) NOT NULL,
                                                   sftp_path VARCHAR(1024) NOT NULL, -- 原始文件在 SFTP 上的路径
                                                   local_temp_path VARCHAR(1024), -- 文件在本地临时目录的路径 (用于解密)
                                                   decrypted_sftp_path VARCHAR(1024), -- 解密后文件在 SFTP 上的路径
                                                   sync_status VARCHAR(20) DEFAULT 'Uploaded' CHECK (sync_status IN ('Uploaded', 'Decrypted', 'Synced', 'Error')), -- 同步状态
                                                   file_type VARCHAR(100), -- 文件 MIME 类型
                                                   file_size BIGINT, -- 文件大小 (字节)
                                                   upload_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                                   uploader VARCHAR(100), -- 上传人 (关联 t_employee.employee)

                                                   CONSTRAINT fk_attachment_task FOREIGN KEY (task_id) REFERENCES public.t_task(task_id) ON DELETE CASCADE ON UPDATE CASCADE -- 任务删除时级联删除附件
);
COMMENT ON TABLE public.t_attachment IS '任务附件信息表';
COMMENT ON COLUMN public.t_attachment.attachment_id IS '附件ID (自增)';
COMMENT ON COLUMN public.t_attachment.task_id IS '所属任务ID (外键)';
COMMENT ON COLUMN public.t_attachment.file_name IS '原始文件名';
COMMENT ON COLUMN public.t_attachment.sftp_path IS '原始文件在 SFTP 上的存储路径';
COMMENT ON COLUMN public.t_attachment.local_temp_path IS '文件在本地临时目录的路径 (解密前)';
COMMENT ON COLUMN public.t_attachment.decrypted_sftp_path IS '解密后文件在 SFTP 上的存储路径';
COMMENT ON COLUMN public.t_attachment.sync_status IS '解密同步状态 (Uploaded, Decrypted, Synced, Error)';
COMMENT ON COLUMN public.t_attachment.file_type IS '文件 MIME 类型';
COMMENT ON COLUMN public.t_attachment.file_size IS '文件大小 (字节)';
COMMENT ON COLUMN public.t_attachment.upload_time IS '上传时间';
COMMENT ON COLUMN public.t_attachment.uploader IS '上传人 (关联 t_employee.employee)';

-- 添加附件表索引
CREATE INDEX IF NOT EXISTS idx_t_attachment_task_id ON public.t_attachment (task_id);
CREATE INDEX IF NOT EXISTS idx_t_attachment_sync_status ON public.t_attachment (sync_status);
CREATE INDEX IF NOT EXISTS idx_t_attachment_file_name ON public.t_attachment (file_name); -- 用于按文件名搜索

-- ----------------------------
-- 6. 项目-标签关联表 (多对多)
-- ----------------------------
-- DROP TABLE IF EXISTS public.t_project_has_tag CASCADE;
CREATE TABLE IF NOT EXISTS public.t_project_has_tag (
                                                        project_id BIGINT NOT NULL,
                                                        tag_id INT NOT NULL,
                                                        PRIMARY KEY (project_id, tag_id), -- 复合主键
                                                        CONSTRAINT fk_pht_project FOREIGN KEY (project_id) REFERENCES public.t_project(project_id) ON DELETE CASCADE ON UPDATE CASCADE,
                                                        CONSTRAINT fk_pht_tag FOREIGN KEY (tag_id) REFERENCES public.t_tag(tag_id) ON DELETE CASCADE ON UPDATE CASCADE
);
COMMENT ON TABLE public.t_project_has_tag IS '项目与标签的关联表 (多对多)';
COMMENT ON COLUMN public.t_project_has_tag.project_id IS '项目ID';
COMMENT ON COLUMN public.t_project_has_tag.tag_id IS '标签ID';

-- ----------------------------
-- 7. 项目-业务类型关联表 (多对多)
-- ----------------------------
-- DROP TABLE IF EXISTS public.t_project_has_business_type CASCADE;
CREATE TABLE IF NOT EXISTS public.t_project_has_business_type (
                                                                  project_id BIGINT NOT NULL,
                                                                  business_type_id INT NOT NULL,
                                                                  PRIMARY KEY (project_id, business_type_id), -- 复合主键
                                                                  CONSTRAINT fk_phbt_project FOREIGN KEY (project_id) REFERENCES public.t_project(project_id) ON DELETE CASCADE ON UPDATE CASCADE,
                                                                  CONSTRAINT fk_phbt_business_type FOREIGN KEY (business_type_id) REFERENCES public.t_business_type(id) ON DELETE CASCADE ON UPDATE CASCADE -- 关联已有的业务类型表
);
COMMENT ON TABLE public.t_project_has_business_type IS '项目与业务类型的关联表 (多对多)';
COMMENT ON COLUMN public.t_project_has_business_type.project_id IS '项目ID';
COMMENT ON COLUMN public.t_project_has_business_type.business_type_id IS '业务类型ID (关联 t_business_type)';


-- =====================================================================
--                      示例数据 (可选)
-- =====================================================================
-- -- t_project 示例
-- INSERT INTO public.t_project (project_name, owner_name, current_stage_id) VALUES
-- ('新一代CRM系统研发', '项目经理A', 2), -- 假设阶段2是需求设计
-- ('官网性能优化', '项目经理B', 3); -- 假设阶段3是部署验证
--
-- -- t_task 示例 (关联 project_id=1)
-- INSERT INTO public.t_task (project_id, task_name, description, status, priority, assignee, stage_id, due_date) VALUES
-- (1, '用户登录模块设计', '设计用户认证流程和界面', '进行中', '高', '10001-张三', 2, '2025-05-15'),
-- (1, '数据库模型设计', '设计项目所需数据库表结构', '待办', '高', '10002-李四', 2, '2025-05-10'),
-- (1, '项目初步规划', '完成项目范围定义和资源评估', '已完成', '中', '10001-张三', 1, '2025-04-30');
--
-- -- t_project_has_tag 示例 (关联 project_id=1)
-- INSERT INTO public.t_project_has_tag (project_id, tag_id) VALUES
-- (1, 1), -- 假设 tag_id=1 是 '核心业务'
-- (1, 2); -- 假设 tag_id=2 是 '创新项目'
--
-- -- t_project_has_business_type 示例 (关联 project_id=1)
-- INSERT INTO public.t_project_has_business_type (project_id, business_type_id) VALUES
-- (1, 1); -- 假设 business_type_id=1 是某个业务类型


-- =====================================================================
--                      维护语句 (注释掉)
-- =====================================================================
-- -- 清空表数据 (注意外键约束和顺序)
-- -- TRUNCATE TABLE public.t_attachment CASCADE;
-- -- TRUNCATE TABLE public.t_project_has_business_type CASCADE;
-- -- TRUNCATE TABLE public.t_project_has_tag CASCADE;
-- -- TRUNCATE TABLE public.t_task CASCADE;
-- -- TRUNCATE TABLE public.t_project CASCADE;
-- -- TRUNCATE TABLE public.t_tag CASCADE;
-- -- TRUNCATE TABLE public.t_project_stage CASCADE;
--
-- -- 删除表 (注意顺序)
-- -- DROP TABLE IF EXISTS public.t_attachment CASCADE;
-- -- DROP TABLE IF EXISTS public.t_project_has_business_type CASCADE;
-- -- DROP TABLE IF EXISTS public.t_project_has_tag CASCADE;
-- -- DROP TABLE IF EXISTS public.t_task CASCADE;
-- -- DROP TABLE IF EXISTS public.t_project CASCADE;
-- -- DROP TABLE IF EXISTS public.t_tag CASCADE;
-- -- DROP TABLE IF EXISTS public.t_project_stage CASCADE;

SELECT '项目管理相关表结构创建完毕';

--
-- **说明:**
--
-- 1.  **表创建:** 使用 `CREATE TABLE IF NOT EXISTS` 避免重复创建错误。
--     2.  **主键:** 使用 `SERIAL` (INT) 或 `BIGSERIAL` (BIGINT) 创建自增主键。对于关联表，使用复合主键。
--     3.  **外键:** 使用 `FOREIGN KEY` 定义表间关系。
--     * `ON DELETE CASCADE`: 当关联的主表记录删除时，从表的相关记录也会被删除（例如，删除项目时删除其所有任务和关联记录）。
--               * `ON DELETE SET NULL`: 当关联的主表记录删除时，从表的外键字段设为 NULL（例如，删除阶段时，项目的当前阶段设为 NULL）。
--     * `ON DELETE RESTRICT`: 如果从表存在关联记录，则阻止删除主表记录（例如，阻止删除仍有任务关联的阶段）。
--               * `ON UPDATE CASCADE`: 当主表主键更新时，从表外键也自动更新（如果主键允许更新的话，通常不建议）。
--                         4.  **约束:** 添加了 `UNIQUE`, `NOT NULL`, `DEFAULT`, `CHECK` 约束来保证数据完整性。
--                         5.  **数据类型:** 选择了合适的 PostgreSQL 数据类型（`VARCHAR`, `TEXT`, `INT`, `BIGINT`, `TIMESTAMP WITH TIME ZONE`, `DATE`, `BOOLEAN`）。
-- 6.  **注释:** 使用 `COMMENT ON TABLE` 和 `COMMENT ON COLUMN` 添加了中文注释。
-- 7.  **索引:** 为所有外键列和经常用于查询条件的列创建了索引 (`CREATE INDEX IF NOT EXISTS`)。
-- 8.  **固定数据:** 为 `t_project_stage` 表插入了 5 条固定的阶段数据，为 `t_tag` 表插入了示例数据。使用 `ON CONFLICT DO NOTHING` 避免重复插入。
-- 9.  **维护语句:** 提供了注释掉的 `TRUNCATE` 和 `DROP TABLE` 语句，方便开发和测试阶段清理数据或表