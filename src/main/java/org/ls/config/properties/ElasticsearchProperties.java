/**
 * 目录结构: ElasticsearchIndexService/src/main/java/org/ls/indexer/config/properties/ElasticsearchProperties.java
 * 文件名称: ElasticsearchProperties.java
 * 开发时间: 2025-05-18 23:40:00 UTC/GMT+08:00
 * 作者: Gemini
 * 代码用途: Elasticsearch 连接和索引配置属性类。
 */
package org.ls.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Elasticsearch 相关配置属性。
 * 通过 {@link ConfigurationProperties} 注解将配置文件中以 "dms.indexer.elasticsearch" 为前缀的属性映射到此类成员。
 */
@Data // Lombok注解，自动生成getter, setter, toString等方法
@Component // 将此类注册为Spring Bean，使其可以被扫描到
@ConfigurationProperties(prefix = "dms.elasticsearch")
public class ElasticsearchProperties {

    /**
     * Elasticsearch 主机名。
     * 对应配置文件中的 dms.indexer.elasticsearch.host
     */
    private String host = "localhost";

    /**
     * Elasticsearch HTTP 端口。
     * 对应配置文件中的 dms.indexer.elasticsearch.port
     */
    private int port = 9200;

    /**
     * Elasticsearch 连接协议 (http 或 https)。
     * 对应配置文件中的 dms.indexer.elasticsearch.scheme
     */
    private String scheme = "https";

    /**
     * Elasticsearch 用户名 (如果启用了安全认证)。
     * 对应配置文件中的 dms.indexer.elasticsearch.username
     */
    private String username;

    /**
     * Elasticsearch 密码 (如果启用了安全认证)。
     * 对应配置文件中的 dms.indexer.elasticsearch.password
     */
    private String password;

    /**
     * 目标 Elasticsearch 索引名称。
     * 对应配置文件中的 dms.indexer.elasticsearch.index-name
     */
    private String indexName = "dms_files";

    /**
     * 是否启用 if_seq_no 和 if_primary_term 进行乐观并发控制。
     * 对应配置文件中的 dms.indexer.elasticsearch.if-seq-no.enabled
     */
    private boolean ifSeqNoEnabled = true;

    // 可根据需要添加更多ES相关配置，例如连接超时、socket超时等
    // private int connectTimeout = 5000; // ms
    // private int socketTimeout = 30000; // ms

    /**
     * CA 证书路径。
     * 对应配置文件中的 dms.indexer.elasticsearch.if-seq-no.enabled
     */
    private String caCertPath;
}
