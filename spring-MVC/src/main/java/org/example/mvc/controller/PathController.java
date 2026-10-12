package org.example.mvc.controller;

import org.example.mvc.common.Result;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 请求映射进阶：Ant 风格通配符、正则占位符、params/headers/consumes 限定条件、
 * HEAD 请求（Spring 自动用 GET 处理器响应）。
 */
@RestController
@RequestMapping("/mvc/path")
public class PathController {

    /** 单层匹配：/mvc/path/ant/a 匹配；/mvc/path/ant/a/b 不匹配 */
    @GetMapping("/ant/*")
    public Result antOne() {
        return Result.ok("命中 /mvc/path/ant/* （单层）");
    }

    /** 多层匹配：/mvc/path/ant/a/b/c 也匹配（更精确者优先） */
    @GetMapping("/ant/**")
    public Result antMore() {
        return Result.ok("命中 /mvc/path/ant/** （多层）");
    }

    /** 正则限制：只匹配数字 id */
    @GetMapping("/regex/{id:\\d+}")
    public Result regexNumber(@PathVariable String id) {
        return Result.ok("数字路径变量：" + id);
    }

    /** 正则限制：只匹配小写字母 */
    @GetMapping("/regex/{name:[a-z]+}")
    public Result regexName(@PathVariable String name) {
        return Result.ok("字母路径变量：" + name);
    }

    /** params 限定：/mvc/path/condition?type=json 才命中 */
    @GetMapping(value = "/condition", params = "type=json")
    public Result conditionJson() {
        return Result.ok("命中 params=type=json 的方法");
    }

    /** params 取反：不带 type 参数时命中 */
    @GetMapping(value = "/condition", params = "!type")
    public Result conditionDefault() {
        return Result.ok("命中 params=!type 的方法");
    }

    /** headers 限定：需要请求头 X-Api-Version: 2 */
    @GetMapping(value = "/condition", params = "type=header", headers = "X-Api-Version=2")
    public Result conditionHeader() {
        return Result.ok("命中 headers=X-Api-Version=2 的方法");
    }

    /** consumes 限定：只处理 Content-Type: application/json 的 POST */
    @PostMapping(value = "/consume", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result consumeJson(@RequestBody Map<String, Object> body) {
        return Result.ok("收到 JSON：" + body);
    }

    /** consumes 限定：只处理表单提交 */
    @PostMapping(value = "/consume", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Result consumeForm(@RequestBody String body) {
        return Result.ok("收到表单：" + body);
    }

    /** HEAD 请求：Spring 会用 GET 处理器响应，只返回头不返回体 */
    @GetMapping("/head-demo")
    public Result headDemo() {
        return Result.ok("用 curl -I 访问该地址可观察 HEAD 请求");
    }
}
