// -----------------------------------------------------------------------------------------
// 文件目录结构: org/ls/indexer/config/ElasticsearchClientConfig.java
// 文件名称: ElasticsearchClientConfig.java
// 开发时间: 2024-05-27
// 作者: [你的名字或团队名称]
// 代码用途: 配置 Elasticsearch Java 客户端，包括 HTTP/HTTPS 连接、认证以及自定义 CA 证书信任。
// 版本历史:
//   1.0 (2024-05-27): 初始版本，支持基本认证和 CA 证书配置。
//   1.1 (2024-05-28): 增强日志输出，添加文件头部注释。
//   1.2 (2024-05-28): 解决 Lambda 表达式中变量非 final 的编译错误。
// -----------------------------------------------------------------------------------------
package org.ls.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.ls.config.properties.ElasticsearchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Configuration
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticsearchClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchClientConfig.class);
    private final ElasticsearchProperties elasticsearchProperties;
    private final ResourceLoader resourceLoader;

    @Autowired
    public ElasticsearchClientConfig(ElasticsearchProperties elasticsearchProperties, ResourceLoader resourceLoader) {
        this.elasticsearchProperties = elasticsearchProperties;
        this.resourceLoader = resourceLoader;
        logger.info("ElasticsearchClientConfig 初始化完成，Elasticsearch 属性已加载。");
    }

    @Bean
    public RestClient restClient() throws Exception {
        logger.info("开始配置 Elasticsearch RestClient...");
        logger.info("Elasticsearch 主机: {}, 端口: {}, Scheme: {}",
                elasticsearchProperties.getHost(),
                elasticsearchProperties.getPort(),
                elasticsearchProperties.getScheme());

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        if (StringUtils.hasText(elasticsearchProperties.getUsername()) && StringUtils.hasText(elasticsearchProperties.getPassword())) {
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(elasticsearchProperties.getUsername(), elasticsearchProperties.getPassword()));
            logger.info("已为 Elasticsearch 配置基本认证，用户: {}", elasticsearchProperties.getUsername());
        } else {
            logger.warn("Elasticsearch 未配置基本认证 (用户名或密码未提供)。");
        }

        SSLContext tempSslContext = null;
        if ("https".equalsIgnoreCase(elasticsearchProperties.getScheme())) {
            logger.info("检测到 Elasticsearch Scheme 为 HTTPS，开始构建 SSLContext...");
            tempSslContext = buildSslContext();
        } else {
            logger.info("Elasticsearch Scheme 为 HTTP，无需构建 SSLContext。");
        }

        final SSLContext finalSslContext = tempSslContext; // 声明一个 final 变量供 lambda 使用

        RestClientBuilder builder = RestClient.builder(
                        new HttpHost(elasticsearchProperties.getHost(), elasticsearchProperties.getPort(), elasticsearchProperties.getScheme()))
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    logger.debug("配置 HttpClient: 设置默认凭据提供者。");
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    if (finalSslContext != null) { // 使用 finalSslContext
                        httpClientBuilder.setSSLContext(finalSslContext); // 使用 finalSslContext
                        logger.info("已为 HttpClient 配置自定义 SSLContext。");
                    } else if ("https".equalsIgnoreCase(elasticsearchProperties.getScheme())) {
                        logger.warn("Elasticsearch Scheme 为 HTTPS，但自定义 SSLContext (例如用于 CA 证书) 未能成功构建或未配置。将使用默认 SSLContext。");
                    }
                    // 可选: 配置 keep-alive 策略, 连接超时等.
                    // httpClientBuilder.setKeepAliveStrategy(...)
                    // httpClientBuilder.setDefaultIOReactorConfig(IOReactorConfig.custom().setSoKeepAlive(true).build());
                    logger.debug("HttpClient 配置回调完成。");
                    return httpClientBuilder;
                });

        // 可选: 配置请求超时
        // builder.setRequestConfigCallback(requestConfigBuilder -> {
        // requestConfigBuilder.setConnectTimeout(5000); // 连接超时时间 (毫秒)
        // requestConfigBuilder.setSocketTimeout(60000); // Socket 超时时间 (毫秒)
        // logger.debug("配置请求超时: 连接超时 {}ms, Socket 超时 {}ms", 5000, 60000);
        // return requestConfigBuilder;
        // });

        logger.info("Elasticsearch RestClient 构建完成。");
        return builder.build();
    }

    private SSLContext buildSslContext() throws Exception {
        String caCertPath = elasticsearchProperties.getCaCertPath();
        if (!StringUtils.hasText(caCertPath)) {
            logger.warn("CA 证书路径 (dms.indexer.elasticsearch.caCertPath) 未在配置中提供。将尝试使用默认的 SSLContext (适用于标准受信任的 CA)。");
            // 对于生产环境，如果需要自定义 CA，这里应该抛出错误或有更严格的检查
            // 例如: throw new IllegalStateException("生产环境下 HTTPS 连接必须配置 CA 证书路径 (elasticsearch.caCertPath)");
            return SSLContexts.createDefault(); // 或者根据安全策略返回 null 或抛异常
        }

        logger.info("尝试从 classpath 路径加载 CA 证书: {}", caCertPath);
        try {
            Resource caCertResource = resourceLoader.getResource(caCertPath);
            if (!caCertResource.exists()) {
                logger.error("错误：在 classpath 位置找不到 CA 证书文件: {}", caCertPath);
                throw new RuntimeException("CA 证书文件未找到: " + caCertPath + "。请确保文件存在于 src/main/resources/ 下的正确路径，并且 application.properties 中的路径配置正确 (例如 classpath:certs/ca/ca.crt)。");
            }

            logger.info("成功定位 CA 证书资源: {}", caCertResource.getDescription());
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate caCertificate;
            try (InputStream caCertInputStream = caCertResource.getInputStream()) {
                caCertificate = (X509Certificate) cf.generateCertificate(caCertInputStream);
                logger.debug("CA 证书已成功从输入流生成。颁发者: {}, 主题: {}", caCertificate.getIssuerX500Principal(), caCertificate.getSubjectX500Principal());
            }

            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null); // 初始化一个空的 keystore
            trustStore.setCertificateEntry("ca", caCertificate); // "ca" 是别名，可以自定义
            logger.debug("CA 证书已添加到 TrustStore，别名 'ca'。");

            SSLContextBuilder sslContextBuilder = SSLContexts.custom()
                    .loadTrustMaterial(trustStore, null); // 信任加载的 CA (第二个参数为 TrustStrategy, null 表示默认信任提供的证书)

            logger.info("已成功构建 SSLContext，并加载了自定义 CA 证书。");
            return sslContextBuilder.build();
        } catch (Exception e) {
            logger.error("为 Elasticsearch 构建 SSLContext 时发生严重错误 (CA 证书路径: {}): {}", caCertPath, e.getMessage(), e);
            // 抛出更具体的异常或包装原始异常，以便上层调用者能更好地处理
            throw new RuntimeException("为 Elasticsearch 构建 SSLContext 失败 (CA 证书: " + caCertPath + "): " + e.getMessage(), e);
        }
    }

    @Bean
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient) {
        logger.info("创建 ElasticsearchTransport...");
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        logger.info("ElasticsearchTransport 创建成功。");
        return transport;
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        logger.info("创建 ElasticsearchClient...");
        ElasticsearchClient client = new ElasticsearchClient(transport);
        logger.info("ElasticsearchClient 创建成功，准备与 Elasticsearch 集群交互。");
        return client;
    }
}
