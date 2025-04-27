


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






-- 插入 t_department 测试数据 (dep_name 需要与 t_wkt 中的数据对应)
-- 注意：manager_id 和 assistant_manager_id 需要在 t_employee 插入后才能设置外键，或者先插入部门再更新员工信息
INSERT INTO public.t_department (id, dep_name, dep_level, manager_id, assistant_manager_id, active, is_statistics) VALUES
                                                                                                                       (100, '科技系统集成部', '一级部门', NULL, NULL, TRUE, TRUE),
                                                                                                                       (101, '科技系统集成部集成设计部', '二级部门', NULL, NULL, TRUE, TRUE),
                                                                                                                       (102, '科技系统集成部管理部', '二级部门', NULL, NULL, TRUE, TRUE),
                                                                                                                       (103, '科技系统集成部运维部', '二级部门', NULL, NULL, TRUE, TRUE),
                                                                                                                       (104, '科技系统集成部集成交付部', '二级部门', NULL, NULL, TRUE, TRUE);




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



-- 插入 t_business_type 测试数据
INSERT INTO public.t_business_type (business_category, business_name, business_description, is_enabled) VALUES
                                                                                                            ('研发类', '核心产品研发', '公司核心软件产品的设计与开发', TRUE),
                                                                                                            ('实施类', '项目实施与交付', '客户现场项目部署、调试与交付', TRUE),
                                                                                                            ('咨询类', '技术咨询服务', '提供专业技术解决方案咨询', TRUE),
                                                                                                            ('运维类', '系统运维支持', '负责系统日常运维和故障处理', TRUE),
                                                                                                            ('管理类', '内部管理支持', '部门内部行政、协调等管理工作', FALSE); -- 测试停用




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




-- 插入 t_profit_center 测试数据 (zone 需要与 t_wkt 中的数据对应)
-- 注意: business_type 等拆分字段的值应根据 zone 的实际内容设置，这里仅为示例
INSERT INTO public.t_profit_center (zone, business_type, region_category, region_name, center_name, business_subcategory, department_name, responsible_person, work_location, custom_zone_remark, is_enabled) VALUES
                                                                                                                                                                                                                  ('基础业务-其他业务版块-BD业务-（支持子公司）-BD科技', '基础业务', '其他业务版块', 'BD业务', '（支持子公司）', 'BD科技', NULL, '张三', '北京', 'BD科技支持', TRUE),
                                                                                                                                                                                                                  ('基础业务-基础业务中台-能力中台-系统集成部', '基础业务', '基础业务中台', '能力中台', '系统集成部', NULL, NULL, '李四', '上海', '能力中台-系统集成', TRUE),
                                                                                                                                                                                                                  ('基础业务-基础业务中台-公司级研发-基础业务中台管理部', '基础业务', '基础业务中台', '公司级研发', '基础业务中台管理部', NULL, NULL, '王五', '广州', '中台管理', TRUE),
                                                                                                                                                                                                                  ('基础业务-大区与网省中心-大区四-冀北中心-业务-大营销咨询业务部', '基础业务', '大区与网省中心', '大区四', '冀北中心', '业务', '大营销咨询业务部', '赵六', '天津', '冀北大营销', TRUE),
                                                                                                                                                                                                                  ('基础业务-其他业务版块-XY业务-（支持子公司）-XY科技', '基础业务', '其他业务版块', 'XY业务', '（支持子公司）', 'XY科技', NULL, '孙七', '深圳', 'XY科技支持', FALSE), -- 测试停用
                                                                                                                                                                                                                  ('基础业务-大区与网省中心-大区三-北京中心-业务', '基础业务', '大区与网省中心', '大区三', '北京中心', '业务', NULL, '周八', '北京', '北京中心业务', TRUE);












-- 其他数据库维护


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
