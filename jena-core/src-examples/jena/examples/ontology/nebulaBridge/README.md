# OWL/RDF -> SPARQL -> NebulaGraph nGQL Demo

这个 demo 基于 Apache Jena，演示以下链路：

1. 使用 OWL/RDF(TTL) 建模站点（Site）、网元（NE）、告警（Alarm）、异常状态特征（AbnormalStatusPattern）。
2. Agent skill 把自然语言意图转成 SPARQL。
3. 将 SPARQL 规则翻译为 NebulaGraph 的 nGQL。
4. 执行 nGQL（本示例使用 mock 客户端，便于离线跑通）。

## 文件

- `jena-core/src-examples/data/ontology_graph_demo.ttl`: 本体模型 + 示例实例数据。
- `jena-core/src-examples/data/ontology_graph_bootstrap.ngql`: 程序启动时自动生成的 NebulaGraph 建图和入图脚本。
- `jena-core/src/main/java/org/apache/jena/examples/ontology/nebulaBridge/OwlSparqlNebulaDemo.java`: 入口类。

## 运行

在仓库根目录执行：

```bash
mvn -pl jena-core -DskipTests compile
java -cp jena-core/target/classes org.apache.jena.examples.ontology.nebulaBridge.OwlSparqlNebulaDemo
```

## 生产化建议

- 将 `NebulaClient` 替换为 nebula-java 官方 client。
- 将 `SparqlToNgqlTranslator` 改造成规则引擎或 AST 转换器（而非字符串匹配）。
- 增加 R2RML/自定义 mapping，把物理数据源（MySQL、Kafka 等）同步成 RDF 与 Nebula 双写。
