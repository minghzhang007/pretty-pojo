package com.lewis.commons.processor;

import com.lewis.commons.annotation.LogProxy;
import com.lewis.commons.annotation.StaticProxy;
import com.squareup.javapoet.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Administrator on 2018/10/4.
 */
@SupportedAnnotationTypes({"com.lewis.commons.annotation.StaticProxy"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class StaticProxyAnnotationProcessor extends AbstractProcessor {
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
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(StaticProxy.class);
        for (Element element : elements) {
            info(element, "element kind:%s", element.getKind());
            TypeElement typeElement = TypeElement.class.cast(element);
            StaticProxy staticProxy = typeElement.getAnnotation(StaticProxy.class);
            if (staticProxy == null) {
                continue;
            }
            List<? extends Element> enclosedElements = typeElement.getEnclosedElements();
            if (enclosedElements == null || enclosedElements.size() == 0) {
                return true;
            }

            String className = typeElement.getSimpleName().toString();
            String packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();

            TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(className + "Proxy")
                    .superclass(ClassName.get((typeElement)))
                    .addModifiers(Modifier.PUBLIC);
            List<MethodSpec> methodSpecs = new ArrayList<>();
            for (Element enclosedElement : enclosedElements) {
                if (enclosedElement.getKind() != ElementKind.METHOD) {
                    continue;
                }
                LogProxy annotation = enclosedElement.getAnnotation(LogProxy.class);
                if (annotation == null) {
                    continue;
                }
                ExecutableElement methodElement = ExecutableElement.class.cast(enclosedElement);
                //MethodSpec methodSpec = getMethodSpec(methodElement);
                methodSpecs.add(getMethodSpec(methodElement));
                //typeBuilder.addMethod(methodSpec);
            }
            if (methodSpecs.size() > 0) {
                for (MethodSpec methodSpec : methodSpecs) {
                    typeBuilder.addMethod(methodSpec);
                }
                JavaFile javaFile = JavaFile.builder(packageName, typeBuilder.build()).build();
                try {
                    javaFile.writeTo(filer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        }
        return false;
    }

    private MethodSpec getMethodSpec(ExecutableElement methodElement) {
        String methodName = methodElement.getSimpleName().toString();
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addAnnotation(Override.class)
                .addModifiers(methodElement.getModifiers())
                .returns(ClassName.get(methodElement.getReturnType()));


        List<? extends VariableElement> parameters = methodElement.getParameters();
        StringBuilder parameterBuilder = new StringBuilder();
        if (parameters != null && parameters.size() > 0) {
            for (VariableElement parameter : parameters) {
                TypeName typeName = ClassName.get(parameter.asType());
                String parameterName = parameter.getSimpleName().toString();
                parameterBuilder.append(parameterName).append(",");
                ParameterSpec parameterSpec = ParameterSpec.builder(typeName, parameterName).build();
                methodBuilder.addParameter(parameterSpec);
            }
            parameterBuilder.deleteCharAt(parameterBuilder.toString().length() - 1);
        }

        methodBuilder
                .addStatement("long begin = $T.currentTimeMillis()", System.class)
                .addStatement("$T result = super.$N($N)", methodElement.getReturnType(), methodName, parameterBuilder.toString())
                .addStatement("long costTime = ($T.currentTimeMillis() - begin)", System.class)
                .addStatement("$T.out.print(costTime)", System.class)
                .addStatement("return result");
        return methodBuilder.build();
    }

    private void info(Element element, String format, Object... args) {
        messager.printMessage(Diagnostic.Kind.NOTE, String.format(format, args), element);
    }
}
