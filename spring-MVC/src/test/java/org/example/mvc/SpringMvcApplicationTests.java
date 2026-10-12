package org.example.mvc;

import org.example.mvc.controller.BindController;
import org.example.mvc.controller.HelloController;
import org.example.mvc.controller.PathController;
import org.example.mvc.controller.UserRestController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用 MockMvc 做「不启动服务器」的单元测试（standaloneSetup：只加载指定 Controller）。
 *
 * <p>想要加载完整容器可用 @SpringBootTest + @AutoConfigureMockMvc。</p>
 */
class SpringMvcApplicationTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new HelloController(), new BindController(),
                        new UserRestController(), new PathController())
                .build();
    }

    /** @RequestParam：必填 + 默认值 + 数组 */
    @Test
    void simpleParam() throws Exception {
        mockMvc.perform(get("/mvc/bind/simple").param("name", "Tom").param("age", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("Tom"))
                .andExpect(jsonPath("$.data.age").value(20));
    }

    /** 缺少必填参数 -> 400 */
    @Test
    void missingRequiredParam() throws Exception {
        mockMvc.perform(get("/mvc/bind/simple"))
                .andExpect(status().isBadRequest());
    }

    /** @ResponseBody 返回字符串 */
    @Test
    void textResponse() throws Exception {
        mockMvc.perform(get("/mvc/hello/text"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("纯文本返回值")));
    }

    /** REST：列表与 404 */
    @Test
    void restApi() throws Exception {
        mockMvc.perform(get("/mvc/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("zhangsan"));

        mockMvc.perform(get("/mvc/api/users/999"))
                .andExpect(status().isNotFound());
    }

    /** 路径正则：数字命中 digits 规则，字母命中 [a-z]+ 规则 */
    @Test
    void regexPathVariable() throws Exception {
        mockMvc.perform(get("/mvc/path/regex/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("数字")));

        mockMvc.perform(get("/mvc/path/regex/abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("字母")));
    }

    /** params 条件映射 */
    @Test
    void paramsCondition() throws Exception {
        mockMvc.perform(get("/mvc/path/condition").param("type", "json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("type=json")));

        mockMvc.perform(get("/mvc/path/condition"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("!type")));
    }

    /** 新增用户（JSON 请求体 -> 201） */
    @Test
    void createUser() throws Exception {
        mockMvc.perform(post("/mvc/api/users")
                        .contentType("application/json")
                        .content("{\"username\":\"wangwu\",\"age\":25}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("wangwu"));
    }
}
