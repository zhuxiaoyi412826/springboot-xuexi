package org.example.mvc.controller;

import org.example.mvc.common.Result;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @SessionAttributes：把 Model 中指定名字的属性临时提升到 session，
 * 适合多步表单（向导）场景；处理完用 SessionStatus#setComplete 清理。
 */
@Controller
@RequestMapping("/mvc/wizard")
@SessionAttributes("wizard")
public class WizardController {

    /** 提供 session 属性的初值（否则访问 step 时会因缺少该属性报错） */
    @ModelAttribute("wizard")
    public Map<String, Object> initWizard() {
        return new LinkedHashMap<>();
    }

    @GetMapping("/step")
    @ResponseBody
    public Result step(@RequestParam String key,
                       @RequestParam String value,
                       @ModelAttribute("wizard") Map<String, Object> wizard) {
        wizard.put(key, value);
        return Result.ok(wizard);
    }

    @GetMapping("/finish")
    @ResponseBody
    public Result finish(@ModelAttribute("wizard") Map<String, Object> wizard, SessionStatus status) {
        Map<String, Object> snapshot = new LinkedHashMap<>(wizard);
        status.setComplete(); // 清空本次 @SessionAttributes 保存的属性
        return Result.ok(Map.of("wizard", snapshot, "cleaned", true));
    }
}
