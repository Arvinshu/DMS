/**
 * 目录: src/main/java/org/ls/mapper/FileSyncMapMapper.java
 * 文件名: FileSyncMapMapper.java
 * 开发时间: 2025-04-29 10:01:00 EDT
 * 作者: Gemini
 * 用途: FileSyncMap 实体对应的数据访问层接口 (MyBatis Mapper)。
 */
package org.ls.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.ls.entity.FileSyncMap;

import java.util.List;

@Mapper // 标记为 MyBatis Mapper 接口
public interface FileSyncMapMapper {

    /**
     * 插入一条新的文件同步记录
     * @param record FileSyncMap 实体对象
     * @return 影响的行数
     */
    int insert(FileSyncMap record);

    /**
     * 根据 ID 更新记录的状态和最后更新时间
     * @param id 记录 ID
     * @param status 新的状态
     * @return 影响的行数
     */
    int updateStatusById(@Param("id") Long id, @Param("status") String status);

    /**
     * 根据临时文件名更新记录的状态和最后更新时间
     * @param tempFilename 临时文件名
     * @param status 新的状态
     * @return 影响的行数
     */
    int updateStatusByTempFilename(@Param("tempFilename") String tempFilename, @Param("status") String status);


    /**
     * 批量更新指定 ID 列表的记录状态
     * @param ids ID 列表
     * @param status 新的状态
     * @return 影响的行数
     */
    int updateStatusForIds(@Param("ids") List<Long> ids, @Param("status") String status);

    /**
     * 根据 ID 删除记录
     * @param id 记录 ID
     * @return 影响的行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据临时文件名删除记录
     * @param tempFilename 临时文件名
     * @return 影响的行数
     */
    int deleteByTempFilename(@Param("tempFilename") String tempFilename);

    /**
     * 根据源文件相对路径和原始文件名删除记录
     * @param relativeDirPath 相对目录路径
     * @param originalFilename 原始文件名
     * @return 影响的行数
     */
    int deleteBySourcePath(@Param("relativeDirPath") String relativeDirPath, @Param("originalFilename") String originalFilename);

    /**
     * 删除指定相对目录下（及其子目录下）的所有记录
     * @param relativeDirPathPrefix 相对目录路径前缀 (例如 "project_a/reports/")
     * @return 影响的行数
     */
    int deleteByRelativePathPrefix(@Param("relativeDirPathPrefix") String relativeDirPathPrefix);

    /**
     * 根据源文件相对路径和原始文件名查询记录
     * @param relativeDirPath 相对目录路径
     * @param originalFilename 原始文件名
     * @return 匹配的 FileSyncMap 实体，如果不存在则返回 null
     */
    FileSyncMap selectBySourcePath(@Param("relativeDirPath") String relativeDirPath, @Param("originalFilename") String originalFilename);

    /**
     * 根据临时文件名查询记录
     * @param tempFilename 临时文件名
     * @return 匹配的 FileSyncMap 实体，如果不存在则返回 null
     */
    FileSyncMap selectByTempFilename(@Param("tempFilename") String tempFilename);

    /**
     * 查询指定状态的所有记录
     * @param status 文件状态
     * @return 匹配的 FileSyncMap 实体列表
     */
    List<FileSyncMap> selectByStatus(@Param("status") String status);

    /**
     * 查询指定相对目录下（及其子目录下）的所有记录
     * @param relativeDirPathPrefix 相对目录路径前缀
     * @return 匹配的 FileSyncMap 实体列表
     */
    List<FileSyncMap> selectByRelativePathPrefix(@Param("relativeDirPathPrefix") String relativeDirPathPrefix);

    /**
     * 检查具有给定临时文件名的记录是否存在
     * @param tempFilename 临时文件名
     * @return 如果存在则返回 true，否则返回 false
     */
    boolean existsByTempFilename(@Param("tempFilename") String tempFilename);

    /**
     * 根据状态统计记录数量
     * @param status 文件状态
     * @return 对应状态的记录数
     */
    long countByStatus(@Param("status") String status);

    /**
     * 查询并锁定（使用 FOR UPDATE SKIP LOCKED）指定数量的 'pending_sync' 状态的记录，
     * 并立即将其状态更新为 'syncing'。
     * 用于并发控制，防止多个同步任务处理同一批文件。
     * @param limit 要获取和锁定的最大记录数
     * @return 被成功锁定并更新状态的 FileSyncMap 实体列表
     */
    List<FileSyncMap> selectAndLockPending(@Param("limit") int limit);

}
