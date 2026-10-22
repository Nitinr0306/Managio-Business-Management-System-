package com.managio.platform.tenancy;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/** Registers the tenancy interceptor and lets controllers declare a {@link BusinessContext} parameter. */
@Configuration
@RequiredArgsConstructor
class TenancyWebConfig implements WebMvcConfigurer {

    private final BusinessAccessInterceptor interceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/api/v1/businesses/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType() == BusinessContext.class;
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mav,
                                          NativeWebRequest request, WebDataBinderFactory binderFactory) {
                Object context = request.getAttribute(BusinessAccessInterceptor.CONTEXT_ATTRIBUTE,
                        RequestAttributes.SCOPE_REQUEST);
                if (context == null) {
                    // A controller asked for a context on a route without {businessId}: a programming error.
                    throw new IllegalStateException("BusinessContext requested outside /api/v1/businesses/{businessId}");
                }
                return context;
            }
        });
    }
}
