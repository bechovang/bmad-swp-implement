package com.storagehub.config;

import com.storagehub.dto.ListQuery;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Binds a {@link ListQuery} controller parameter from the {@code page} /
 * {@code pageSize} query-string params with the 1/25 defaults (AD-8). Bad
 * input (non-numeric or < 1) surfaces as MethodArgumentTypeMismatchException
 * so GlobalExceptionHandler answers 400 MALFORMED_REQUEST, never a 500.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new ListQueryArgumentResolver());
    }

    static final class ListQueryArgumentResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return ListQuery.class == parameter.getParameterType();
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            String page = webRequest.getParameter("page");
            String pageSize = webRequest.getParameter("pageSize");
            try {
                return ListQuery.of(page, pageSize);
            }
            catch (IllegalArgumentException ex) {
                String raw = page != null ? page : pageSize;
                throw new MethodArgumentTypeMismatchException(raw, ListQuery.class, "page/pageSize", parameter, ex);
            }
        }
    }
}
