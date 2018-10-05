package com.lewis.commons.processor;

import com.lewis.commons.annotation.LogProxy;
import com.squareup.javapoet.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Created by Administrator on 2018/10/1.
 */
//@SupportedAnnotationTypes({"com.lewis.commons.annotation.LogProxy"})
//@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class LogProxyAnnotationProcessor extends AbstractProcessor {

    private Elements elementUtils;
    private Filer filer;
    private Messager messager;
    private Types typeUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
        typeUtils = processingEnv.getTypeUtils();

    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(LogProxy.class);
        for (Element element : elements) {
            ExecutableElement methodElement = ExecutableElement.class.cast(element);
            TypeElement typeElement = TypeElement.class.cast(methodElement.getEnclosingElement());
            String packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
            String className = methodElement.getEnclosingElement().getSimpleName().toString();
            String methodName = methodElement.getSimpleName().toString();
            info(methodElement, "methodName:%s", methodElement.getSimpleName());
            info(methodElement, "methodName returnTypeName:%s", methodElement.getReturnType());

            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                    .addAnnotation(Override.class)
                    .addModifiers(methodElement.getModifiers())
                    .returns(ClassName.get(methodElement.getReturnType()));


            List<? extends VariableElement> parameters = methodElement.getParameters();
            StringBuilder parameterBuilder = new StringBuilder();
            for (VariableElement parameter : parameters) {
                TypeName typeName = ClassName.get(parameter.asType());
                String parameterName = parameter.getSimpleName().toString();
                parameterBuilder.append(parameterName).append(",");
                ParameterSpec parameterSpec = ParameterSpec.builder(typeName, parameterName).build();
                methodBuilder.addParameter(parameterSpec);
            }
            parameterBuilder.deleteCharAt(parameterBuilder.toString().length() - 1);
            methodBuilder
                    .addStatement("long begin = $T.currentTimeMillis()", System.class)
                    .addStatement("$T result = super.$N($N)", methodElement.getReturnType(), methodName, parameterBuilder.toString())
                    .addStatement("long costTime = ($T.currentTimeMillis() - begin)", System.class)
                    .addStatement("$T.out.print(costTime)", System.class)

                    .addStatement("return result");
            TypeSpec typeSpec = TypeSpec.classBuilder(className + "Proxy")
                    .superclass(ClassName.get((TypeElement) methodElement.getEnclosingElement()))
                    .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                    .addMethod(methodBuilder.build())
                    .build();

            JavaFile javaFile = JavaFile.builder(packageName, typeSpec).build();
            try {
                javaFile.writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
        return false;
    }

    private void info(Element element, String format, Object... args) {
        messager.printMessage(Diagnostic.Kind.NOTE, String.format(format, args), element);
    }

    private void warn(Element element, String format, Object... args) {
        messager.printMessage(Diagnostic.Kind.NOTE, String.format(format, args), element);
    }

    private void error(Element element, String format, Object... args) {
        messager.printMessage(Diagnostic.Kind.NOTE, String.format(format, args), element);
    }
}
