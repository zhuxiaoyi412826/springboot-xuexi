package org.example.mvc.http;

import org.example.mvc.model.User;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 自定义 HttpMessageConverter：让 User 支持 "text/x-user" 这种自定义媒体类型。
 *
 * <p>报文格式：{@code 用户名:年龄:昵称}，例如 {@code zhangsan:20:张三}</p>
 */
public class UserTextHttpMessageConverter extends AbstractHttpMessageConverter<User> {

    public UserTextHttpMessageConverter() {
        super(new MediaType("text", "x-user", StandardCharsets.UTF_8));
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return User.class.isAssignableFrom(clazz);
    }

    @Override
    protected User readInternal(Class<? extends User> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        String body = StreamUtils.copyToString(inputMessage.getBody(), StandardCharsets.UTF_8);
        String[] arr = body.split(":");
        User user = new User();
        user.setUsername(arr.length > 0 ? arr[0] : "");
        if (arr.length > 1) {
            user.setAge(Integer.valueOf(arr[1].trim()));
        }
        user.setNickname(arr.length > 2 ? arr[2].trim() : user.getUsername());
        return user;
    }

    @Override
    protected void writeInternal(User user, HttpOutputMessage outputMessage) throws IOException {
        String out = user.getUsername() + ":" + user.getAge() + ":" + user.getNickname();
        StreamUtils.copy(out, StandardCharsets.UTF_8, outputMessage.getBody());
    }
}
