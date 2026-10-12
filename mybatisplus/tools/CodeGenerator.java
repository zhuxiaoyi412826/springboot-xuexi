import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.sql.Types;
import java.util.Collections;

/**
 * MyBatis-Plus 代码生成器（独立工具，故意放在 tools/ 下，不参与项目编译）。
 *
 * <p><b>怎么跑</b>（二选一）：
 * <ol>
 *   <li>JDK 21 单文件源码启动（推荐，不用装依赖）：
 *       <pre>
 *       mvn -B dependency:build-classpath -Dmdep.outputFile=tools/cp.txt
 *       java -cp "$(cat tools/cp.txt);target/classes" tools/CodeGenerator.java
 *       </pre>
 *   </li>
 *   <li>先把生成器依赖临时加进 pom，再直接运行 main 方法</li>
 * </ol>
 *
 * <p>需要的额外依赖（生成器不在 mybatis-plus-spring-boot4-starter 里）：
 * <pre>
 *   com.baomidou:mybatis-plus-generator:3.5.17
 *   org.freemarker:freemarker:2.3.32   （模板引擎，也可以换成 velocity）
 * </pre>
 *
 * <p>生成后请把代码 review 一遍：类名、包名、是否覆盖已有文件都要确认。
 */
public class CodeGenerator {

    /** 输出目录（项目根目录） */
    private static final String OUTPUT_DIR = System.getProperty("user.dir");

    /** 父包名 */
    private static final String PARENT_PACKAGE = "org.example.mybatisplus";

    /** 要生成的表（不写就是全库） */
    private static final String[] INCLUDE_TABLES = {"t_bike", "t_order", "t_user", "t_tenant_goods", "t_batch_demo"};

    public static void main(String[] args) {
        FastAutoGenerator.create(
                        "jdbc:mysql://127.0.0.1:3306/bike_shop?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai",
                        "root",
                        "412826")
                .globalConfig(builder -> builder
                        .author("mybatisplus-demo")
                        .commentDate("yyyy-MM-dd")
                        .outputDir(OUTPUT_DIR + "/src/main/java")
                        // 生成完不要自动打开文件夹
                        .disableOpenDir())
                .dataSourceConfig(builder -> builder
                        // 把数据库的 datetime 映射成 LocalDateTime（而不是 java.util.Date）
                        .typeConvertHandler((globalConfig, typeRegistry, metaInfo) -> {
                            int typeCode = metaInfo.getJdbcType().TYPE_CODE;
                            if (typeCode == Types.TIMESTAMP || typeCode == Types.DATE || typeCode == Types.TIME) {
                                return DbColumnType.LOCAL_DATE_TIME;
                            }
                            return typeRegistry.getColumnType(metaInfo);
                        }))
                .packageConfig(builder -> builder
                        .parent(PARENT_PACKAGE)
                        .entity("entity")
                        .mapper("mapper")
                        .service("service")
                        .serviceImpl("service.impl")
                        .controller("controller")
                        .xml("mapper.xml")
                        .pathInfo(Collections.singletonMap(OutputFile.xml, OUTPUT_DIR + "/src/main/resources/mapper")))
                .strategyConfig(builder -> builder
                        .addInclude(INCLUDE_TABLES)
                        // 表名前缀 t_ 去掉：t_bike → Bike
                        .addTablePrefix("t_")
                        .entityBuilder()
                        .enableLombok()
                        .enableTableFieldAnnotation()
                        .logicDeleteColumnName("deleted")
                        .versionColumnName("version")
                        .controllerBuilder()
                        .enableRestStyle()
                        .mapperBuilder()
                        .enableMapperAnnotation())
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }
}
