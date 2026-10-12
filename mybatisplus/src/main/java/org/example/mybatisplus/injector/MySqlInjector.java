package org.example.mybatisplus.injector;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.extension.injector.methods.InsertBatchSomeColumn;
import org.apache.ibatis.session.Configuration;

import java.util.List;

/**
 * 自定义 SQL 注入器：在 BaseMapper 自带方法之外，再注入一个「真正的批量插入」。
 *
 * <p>默认的 saveBatch 走的是 JDBC Batch（多条 INSERT 打包发送），
 * 而 InsertBatchSomeColumn 生成的是一条 {@code INSERT INTO t (...) VALUES (...),(...),(...)}，
 * 配合 JDBC URL 上的 {@code rewriteBatchedStatements=true}，条数越多差距越明显。
 */
public class MySqlInjector extends DefaultSqlInjector {

    @Override
    public List<AbstractMethod> getMethodList(Configuration configuration, Class<?> mapperClass, TableInfo tableInfo) {
        List<AbstractMethod> methods = super.getMethodList(configuration, mapperClass, tableInfo);
        // 只在 insert 时填充的字段才参与批量插入（跳过 update_time 这类字段）
        methods.add(new InsertBatchSomeColumn(field -> field.getFieldFill() != FieldFill.UPDATE));
        return methods;
    }
}
