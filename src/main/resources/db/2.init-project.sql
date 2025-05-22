-- 文件路径: src/main/resources/db/project.sql
-- 描述: 创建项目管理模块所需的数据表和初始数据

-- 删除旧表 (如果存在，用于重新初始化)
DROP TABLE IF EXISTS public.t_task CASCADE;
DROP TABLE IF EXISTS public.t_project_project_tag CASCADE;
DROP TABLE IF EXISTS public.t_project CASCADE;
DROP TABLE IF EXISTS public.t_project_tag CASCADE;
DROP TABLE IF EXISTS public.t_project_stage CASCADE;

-- 1. 项目阶段表 (t_project_stage)
CREATE TABLE IF NOT EXISTS public.t_project_stage
(
    stage_id          SERIAL PRIMARY KEY,                  -- 阶段ID (自增主键)
    stage_order       INTEGER NOT NULL DEFAULT 0,          -- 阶段排序号
    stage_name        VARCHAR(100) NOT NULL UNIQUE,        -- 阶段名称 (唯一)
    stage_description TEXT,                                -- 阶段描述
    is_enabled        BOOLEAN NOT NULL DEFAULT TRUE,       -- 是否可用
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    updated_at        TIMESTAMP WITH TIME ZONE             -- 更新时间 (由应用程序维护)
);

-- 表注释
COMMENT ON TABLE public.t_project_stage IS '项目工作阶段定义表';
-- 列注释
COMMENT ON COLUMN public.t_project_stage.stage_id IS '阶段唯一标识符';
COMMENT ON COLUMN public.t_project_stage.stage_order IS '用于前端显示的排序顺序';
COMMENT ON COLUMN public.t_project_stage.stage_name IS '项目阶段的名称';
COMMENT ON COLUMN public.t_project_stage.stage_description IS '对项目阶段的详细描述';
COMMENT ON COLUMN public.t_project_stage.is_enabled IS '标识该阶段是否在系统中启用';
COMMENT ON COLUMN public.t_project_stage.created_at IS '记录创建时间';
COMMENT ON COLUMN public.t_project_stage.updated_at IS '记录最后更新时间';

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_project_stage_order ON public.t_project_stage (stage_order);
CREATE INDEX IF NOT EXISTS idx_project_stage_name ON public.t_project_stage (stage_name); -- 支持按名称查询/排序
CREATE INDEX IF NOT EXISTS idx_project_stage_enabled ON public.t_project_stage (is_enabled); -- 支持按启用状态查询

-- 初始化项目阶段数据
INSERT INTO public.t_project_stage (stage_order, stage_name, stage_description, is_enabled) VALUES
                                                                                                (10, '策划', '项目初步规划和可行性分析阶段', TRUE),
                                                                                                (20, '需求设计', '详细需求分析和系统设计阶段', TRUE),
                                                                                                (30, '开发执行', '编码实现和单元测试阶段', TRUE), -- 补充一个开发阶段
                                                                                                (40, '部署验证', '系统部署和集成测试、用户验收测试阶段', TRUE),
                                                                                                (50, '上线验收', '系统正式上线和最终用户验收阶段', TRUE),
                                                                                                (60, '系统运维', '系统上线后的维护和支持阶段', TRUE);

-- 2. 项目标签表 (t_project_tag)
CREATE TABLE IF NOT EXISTS public.t_project_tag
(
    tag_id     BIGSERIAL PRIMARY KEY,                 -- 标签ID (自增主键)
    tag_name   VARCHAR(100) NOT NULL UNIQUE,        -- 标签名称 (唯一)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    updated_at TIMESTAMP WITH TIME ZONE             -- 更新时间 (由应用程序维护)
);

-- 表注释
COMMENT ON TABLE public.t_project_tag IS '项目标签定义表';
-- 列注释
COMMENT ON COLUMN public.t_project_tag.tag_id IS '标签唯一标识符';
COMMENT ON COLUMN public.t_project_tag.tag_name IS '标签的名称，用于项目分类和搜索';
COMMENT ON COLUMN public.t_project_tag.created_at IS '记录创建时间';
COMMENT ON COLUMN public.t_project_tag.updated_at IS '记录最后更新时间';

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_project_tag_name ON public.t_project_tag (tag_name); -- 支持按名称查询/排序

-- (可选) 初始化一些常用标签
-- INSERT INTO public.t_project_tag (tag_name) VALUES ('核心业务'), ('创新项目'), ('内部优化');

-- 3. 项目主表 (t_project)
CREATE TABLE IF NOT EXISTS public.t_project
(
    project_id               BIGSERIAL PRIMARY KEY,                 -- 项目ID (自增主键)
    project_name             VARCHAR(255) NOT NULL UNIQUE,        -- 项目名称 (唯一)
    project_description      TEXT,                                -- 项目描述
    business_type_name       VARCHAR(200),                        -- 项目业务类型 (单选, 关联 t_business_type.business_name)
    profit_center_zone       VARCHAR(200),                        -- 项目利润中心 (关联 t_profit_center.zone)
    project_manager_employee VARCHAR(30),                         -- 项目负责人 (关联 t_employee.employee)
    ts_bm                    VARCHAR(200),                        -- 项目工时代码 (关联 t_timesheet_code.ts_bm)
    created_at               TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    updated_at               TIMESTAMP WITH TIME ZONE,            -- 更新时间 (由应用程序维护)
    created_by               VARCHAR(30),                         -- 创建人 (关联 t_employee.employee)
    updated_by               VARCHAR(30)                          -- 更新人 (关联 t_employee.employee)
);

-- 表注释
COMMENT ON TABLE public.t_project IS '项目信息主表';
-- 列注释
COMMENT ON COLUMN public.t_project.project_id IS '项目唯一标识符';
COMMENT ON COLUMN public.t_project.project_name IS '项目的名称';
COMMENT ON COLUMN public.t_project.project_description IS '项目的详细描述';
COMMENT ON COLUMN public.t_project.business_type_name IS '关联的业务类型名称';
COMMENT ON COLUMN public.t_project.profit_center_zone IS '关联的利润中心区域代码';
COMMENT ON COLUMN public.t_project.project_manager_employee IS '关联的项目负责人员工信息';
COMMENT ON COLUMN public.t_project.ts_bm IS '关联的工时代码';
COMMENT ON COLUMN public.t_project.created_at IS '记录创建时间';
COMMENT ON COLUMN public.t_project.updated_at IS '记录最后更新时间';
COMMENT ON COLUMN public.t_project.created_by IS '创建该记录的员工';
COMMENT ON COLUMN public.t_project.updated_by IS '最后更新该记录的员工';

-- 添加外键约束 (推荐，如果数据库支持且需要强制关联)
ALTER TABLE public.t_project
    ADD CONSTRAINT fk_project_business_type FOREIGN KEY (business_type_name) REFERENCES public.t_business_type(business_name) ON DELETE SET NULL ON UPDATE CASCADE,
    ADD CONSTRAINT fk_project_profit_center FOREIGN KEY (profit_center_zone) REFERENCES public.t_profit_center(zone) ON DELETE SET NULL ON UPDATE CASCADE,
    ADD CONSTRAINT fk_project_manager FOREIGN KEY (project_manager_employee) REFERENCES public.t_employee(employee) ON DELETE SET NULL ON UPDATE CASCADE,
    ADD CONSTRAINT fk_project_ts_code FOREIGN KEY (ts_bm) REFERENCES public.t_timesheet_code(ts_bm) ON DELETE SET NULL ON UPDATE CASCADE,
    ADD CONSTRAINT fk_project_created_by FOREIGN KEY (created_by) REFERENCES public.t_employee(employee) ON DELETE SET NULL ON UPDATE CASCADE,
    ADD CONSTRAINT fk_project_updated_by FOREIGN KEY (updated_by) REFERENCES public.t_employee(employee) ON DELETE SET NULL ON UPDATE CASCADE;

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_project_name ON public.t_project (project_name text_pattern_ops); -- 支持前缀模糊查询
CREATE INDEX IF NOT EXISTS idx_project_business_type ON public.t_project (business_type_name);
CREATE INDEX IF NOT EXISTS idx_project_profit_center ON public.t_project (profit_center_zone);
CREATE INDEX IF NOT EXISTS idx_project_manager ON public.t_project (project_manager_employee);
CREATE INDEX IF NOT EXISTS idx_project_ts_bm ON public.t_project (ts_bm);
CREATE INDEX IF NOT EXISTS idx_project_created_at ON public.t_project (created_at DESC); -- 按创建时间倒序

-- 4. 项目-标签映射表 (t_project_project_tag)
CREATE TABLE IF NOT EXISTS public.t_project_project_tag
(
    project_id BIGINT NOT NULL,
    tag_id     BIGINT NOT NULL,
    PRIMARY KEY (project_id, tag_id), -- 联合主键确保唯一性
    CONSTRAINT fk_ppt_project FOREIGN KEY (project_id) REFERENCES public.t_project(project_id) ON DELETE CASCADE, -- 项目删除时级联删除关联
    CONSTRAINT fk_ppt_tag FOREIGN KEY (tag_id) REFERENCES public.t_project_tag(tag_id) ON DELETE CASCADE       -- 标签删除时级联删除关联
);

-- 表注释
COMMENT ON TABLE public.t_project_project_tag IS '项目与标签的多对多关系映射表';
-- 列注释
COMMENT ON COLUMN public.t_project_project_tag.project_id IS '关联的项目ID';
COMMENT ON COLUMN public.t_project_project_tag.tag_id IS '关联的标签ID';

-- 添加索引 (用于优化JOIN查询)
CREATE INDEX IF NOT EXISTS idx_ppt_project_id ON public.t_project_project_tag (project_id);
CREATE INDEX IF NOT EXISTS idx_ppt_tag_id ON public.t_project_project_tag (tag_id);

-- 5. 任务表 (t_task)
CREATE TABLE IF NOT EXISTS public.t_task
(
    task_id          BIGSERIAL PRIMARY KEY,                 -- 任务ID (自增主键)
    project_id       BIGINT NOT NULL,                       -- 所属项目ID
    task_name        VARCHAR(255) NOT NULL,               -- 任务名称
    task_description TEXT,                                -- 任务描述
    priority         VARCHAR(20) DEFAULT 'Medium',        -- 优先级 ('High', 'Medium', 'Low')
    assignee_employee VARCHAR(30),                         -- 分配给 (关联 t_employee.employee)
    start_date       DATE,                                -- 开始日期
    due_date         DATE,                                -- 截止日期
    stage_id         INTEGER NOT NULL,                      -- 所属阶段ID
    task_status      VARCHAR(20) DEFAULT '待办' NOT NULL, -- 任务状态 ('待办', '进行中', '已完成', '已暂停', '已取消')
    attachments      TEXT,                                -- 附件信息 (简单存储，如JSON或逗号分隔的文件名)
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    updated_at       TIMESTAMP WITH TIME ZONE,            -- 更新时间 (由应用程序维护)
    CONSTRAINT fk_task_project FOREIGN KEY (project_id) REFERENCES public.t_project(project_id) ON DELETE CASCADE, -- 项目删除时级联删除任务
    CONSTRAINT fk_task_stage FOREIGN KEY (stage_id) REFERENCES public.t_project_stage(stage_id) ON DELETE RESTRICT, -- 阶段通常不应随意删除，故使用RESTRICT
    CONSTRAINT fk_task_assignee FOREIGN KEY (assignee_employee) REFERENCES public.t_employee(employee) ON DELETE SET NULL ON UPDATE CASCADE
);

-- 表注释
COMMENT ON TABLE public.t_task IS '项目任务信息表';
-- 列注释
COMMENT ON COLUMN public.t_task.task_id IS '任务唯一标识符';
COMMENT ON COLUMN public.t_task.project_id IS '该任务所属的项目ID';
COMMENT ON COLUMN public.t_task.task_name IS '任务的名称';
COMMENT ON COLUMN public.t_task.task_description IS '任务的详细描述';
COMMENT ON COLUMN public.t_task.priority IS '任务的优先级';
COMMENT ON COLUMN public.t_task.assignee_employee IS '任务分配给的员工';
COMMENT ON COLUMN public.t_task.start_date IS '任务计划开始日期';
COMMENT ON COLUMN public.t_task.due_date IS '任务计划截止日期';
COMMENT ON COLUMN public.t_task.stage_id IS '任务当前所属的项目阶段ID';
COMMENT ON COLUMN public.t_task.task_status IS '任务的当前状态';
COMMENT ON COLUMN public.t_task.attachments IS '与任务相关的附件信息';
COMMENT ON COLUMN public.t_task.created_at IS '记录创建时间';
COMMENT ON COLUMN public.t_task.updated_at IS '记录最后更新时间';

-- 添加索引
CREATE INDEX IF NOT EXISTS idx_task_project_id ON public.t_task (project_id);
CREATE INDEX IF NOT EXISTS idx_task_stage_id ON public.t_task (stage_id);
CREATE INDEX IF NOT EXISTS idx_task_assignee ON public.t_task (assignee_employee);
CREATE INDEX IF NOT EXISTS idx_task_status ON public.t_task (task_status);
CREATE INDEX IF NOT EXISTS idx_task_due_date ON public.t_task (due_date);
CREATE INDEX IF NOT EXISTS idx_task_updated_at ON public.t_task (updated_at DESC); -- 用于查询最新任务

-- 设置表所有者 (根据实际情况修改用户名)
ALTER TABLE public.t_project_stage OWNER TO postgres;
ALTER TABLE public.t_project_tag OWNER TO postgres;
ALTER TABLE public.t_project OWNER TO postgres;
ALTER TABLE public.t_project_project_tag OWNER TO postgres;
ALTER TABLE public.t_task OWNER TO postgres;

SELECT '项目管理模块数据库表结构创建和初始化完成';


-- truncate TABLE public.t_task CASCADE;
-- truncate TABLE public.t_project_project_tag CASCADE;
-- truncate TABLE public.t_project CASCADE;
-- truncate TABLE public.t_project_tag CASCADE;
-- truncate TABLE public.t_project_stage CASCADE;

