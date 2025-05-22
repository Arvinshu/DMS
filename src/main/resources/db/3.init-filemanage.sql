-- 文件同步功能数据库初始化脚本

-- 1. 创建表: file_sync_map
-- 用于存储加密文件到临时文件的映射关系以及同步状态
CREATE TABLE IF NOT EXISTS file_sync_map (
                                             id SERIAL PRIMARY KEY,
                                             relative_dir_path VARCHAR(1024) NOT NULL,
                                             original_filename VARCHAR(255) NOT NULL,
                                             temp_filename VARCHAR(300) UNIQUE NOT NULL,
                                             status VARCHAR(50) NOT NULL DEFAULT 'pending_sync',
                                             source_last_modified TIMESTAMP NULL, -- 允许初始为 NULL
                                             last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 表注释
COMMENT ON TABLE file_sync_map IS '文件同步映射表，记录源文件、临时文件和同步状态';

-- 字段注释
COMMENT ON COLUMN file_sync_map.id IS '唯一主键ID';
COMMENT ON COLUMN file_sync_map.relative_dir_path IS '源文件相对于加密目录根目录的相对路径 (例如 projects/alpha/docs/ 或空字符串 '' 表示根目录)';
COMMENT ON COLUMN file_sync_map.original_filename IS '源文件在加密目录中的原始文件名';
COMMENT ON COLUMN file_sync_map.temp_filename IS '文件在临时目录中的唯一名称 (可能带后缀)';
COMMENT ON COLUMN file_sync_map.status IS '文件同步状态 (pending_sync, synced, error_copying, error_syncing, syncing)';
COMMENT ON COLUMN file_sync_map.source_last_modified IS '源文件在加密目录中的最后修改时间戳';
COMMENT ON COLUMN file_sync_map.last_updated IS '记录最后更新时间戳';


-- 2. 创建索引
-- 提高按源路径+文件名查询的性能
CREATE INDEX IF NOT EXISTS idx_fsmap_path_file ON file_sync_map (relative_dir_path, original_filename);
-- 提高按状态查询的性能
CREATE INDEX IF NOT EXISTS idx_fsmap_status ON file_sync_map (status);
-- temp_filename 的唯一约束通常会自动创建索引，无需手动创建


-- 3. 示例数据 (注释掉，需要时取消注释)
/*
INSERT INTO file_sync_map (relative_dir_path, original_filename, temp_filename, status, last_updated) VALUES
('', 'root_document.enc', 'root_document.enc', 'pending_sync', NOW()),
('project_a/reports/', 'q1_report.enc', 'q1_report.enc', 'pending_sync', NOW()),
('project_a/reports/', 'q2_report.enc', 'q2_report.enc', 'synced', NOW() - interval '1 day'),
('project_b/images/', 'logo.png.enc', 'logo.png.enc', 'pending_sync', NOW()),
('project_b/images/', 'logo.png.enc', 'logo.png_1.enc', 'error_copying', NOW() - interval '2 hours'), -- 示例：同名文件复制错误
('project_c/', 'important_notes.txt.enc', 'important_notes.txt.enc', 'error_syncing', NOW() - interval '3 hours'); -- 示例：同步移动时出错
*/


-- 4. 清空表示例数据 (注释掉，需要时取消注释)
-- TRUNCATE TABLE file_sync_map RESTART IDENTITY;


-- 5. 删除表 (注释掉，需要时取消注释)
-- DROP TABLE IF EXISTS file_sync_map;
