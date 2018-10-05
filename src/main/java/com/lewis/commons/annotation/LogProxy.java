package com.lewis.commons.annotation;

import java.lang.annotation.*;

/**
 * Created by Administrator on 2018/10/1.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD})
@Inherited
public @interface LogProxy {
}
