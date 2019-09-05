//package com.example.chess.server.core;
//
//import net.bytebuddy.dynamic.scaffold.InstrumentedType;
//
///**
// * @author v.peschaniy
// * Date: 05.09.2019
// */
//public class Ex {
//    Code generation
//
// [group: 'net.bytebuddy', name: 'byte-buddy', version: '1.9.13']
//
//            package com.my.games.shelter.core.codegeneration;
//
//import net.bytebuddy.ByteBuddy;
//import net.bytebuddy.description.annotation.AnnotationDescription;
//import net.bytebuddy.description.method.MethodDescription;
//import net.bytebuddy.description.modifier.Visibility;
//import net.bytebuddy.description.type.TypeDescription;
//import net.bytebuddy.dynamic.DynamicType;
//import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
//import net.bytebuddy.dynamic.scaffold.InstrumentedType;
//import net.bytebuddy.implementation.MethodCall;
//import net.bytebuddy.implementation.bytecode.assign.Assigner;
//import net.bytebuddy.jar.asm.Opcodes;
//import org.springframework.beans.BeansException;
//import org.springframework.beans.factory.config.BeanDefinition;
//import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
//import org.springframework.beans.factory.support.BeanDefinitionRegistry;
//import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
//import org.springframework.beans.factory.support.RootBeanDefinition;
//import org.springframework.context.ConfigurableApplicationContext;
//import ru.mail.games.core.remoting.IRemoteRequestAgent;
//import ru.mail.games.core.remoting.impl.IRemoteHandler;
//
//import javax.annotation.Resource;
//import java.io.IOException;
//import java.io.Serializable;
//import java.lang.annotation.Retention;
//import java.lang.annotation.RetentionPolicy;
//import java.lang.reflect.Method;
//import java.util.*;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//import static net.bytebuddy.matcher.ElementMatchers.named;
//
//    public class HandlerGenerator implements BeanDefinitionRegistryPostProcessor {
//
//        public static void main(String[] args) throws IllegalAccessException, IOException, InstantiationException {
//            new ru.mail.games.core.codegeneration.HandlerGenerator().generate(SomeManager.class);
//        }
//
//        public Result generate(Class<?> clazz) throws IllegalAccessException, InstantiationException, IOException {
//
//            ru.mail.games.core.codegeneration.HandlerGenerator generator = new ru.mail.games.core.codegeneration.HandlerGenerator();
//
//            List<Method> methods = Arrays.stream(clazz.getDeclaredMethods())
//                    .filter(m -> m.isAnnotationPresent(Player.class))
//                    .collect(Collectors.toList());
//
//            if (methods.isEmpty()) {
//                return null;
//            }
//
//            InstrumentedType managerType = InstrumentedType.Default.of(
//                    clazz.getPackage().getName() + "." + clazz.getSimpleName() + "Instrumented",
//                    new TypeDescription.Generic.OfNonGenericType.ForLoadedType(clazz),
//                    Opcodes.ACC_PUBLIC
//            );
//
//            DynamicType.Builder<?> instrumentedManager = new ByteBuddy()
//                    .subclass(clazz)
//                    .name(managerType.getName());
//
//            List<DynamicType.Unloaded<?>> instrumentedHandlers = new ArrayList<>();
//            for (int i = 0; i < methods.size(); i++) {
//                Method method = methods.get(i);
//
//                managerType = managerType.withMethod(new MethodDescription.Token(
//                        method.getName() + "$origin",
//                        Opcodes.ACC_PUBLIC,
//                        new TypeDescription.Generic.OfNonGenericType.ForLoadedType(method.getReturnType()),
//                        Arrays.stream(method.getParameterTypes())
//                                .map(TypeDescription.Generic.OfNonGenericType.ForLoadedType::new)
//                                .collect(Collectors.toList())
//                ));
//
//                DynamicType.Unloaded<?> instrumentedHandler = new ByteBuddy()
//                        .subclass(BaseHandler.class)
//                        .name(clazz.getPackage().getName() + "." + method.getName() + "Handler")
//                        .defineField("manager", managerType)
//                        .annotateField(AnnotationDescription.Builder.ofType(Resource.class).build())
//                        .defineMethod("prepare", void.class, Visibility.PUBLIC)
//                        .withParameter(Object[].class, "arguments")
//                        .intercept(MethodCall
//                                .invoke(named("remoteLogic")).onSuper()
//                                .withArgument(0))
//
//                        .method(named("execute"))
//                        .intercept(MethodCall
//                                .invoke(named(method.getName() + "$origin"))
//                                .onField("manager")
//                                .withArgumentArrayElements(0, 0, 2)
//                                .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
//                        .make();
//                instrumentedHandlers.add(instrumentedHandler);
////            instrumentedHandler.saveIn(new File("./out"));
//
//                instrumentedManager = instrumentedManager
//                        .defineField(method.getName() + "Handler", instrumentedHandler.getTypeDescription(), Visibility.PUBLIC)
//                        .annotateField(AnnotationDescription.Builder.ofType(Resource.class).build())
//
//                        .define(method)
//                        .intercept(MethodCall.invoke(named("prepare")).onField(method.getName() + "Handler").withArgumentArray())
//
//                        .defineMethod(method.getName() + "$origin", void.class, Visibility.PUBLIC)
//                        .withParameters(method.getParameterTypes())
//                        .intercept(MethodCall.invoke(method).onSuper().withAllArguments());
//            }
//
//            DynamicType.Unloaded<?> managerUnloadedClass = instrumentedManager.make();
////        managerUnloadedClass.saveIn(new File("./out"));
//
//            Class<?> managerClass = managerUnloadedClass.load(getClass().getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
//                    .getLoaded();
//
//            List<Class<?>> handlers = new ArrayList<>();
//            instrumentedHandlers.forEach(instrumentedHandler -> {
//                try {
//                    Class<?> handlerClass = instrumentedHandler
//                            .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
//                            .getLoaded();
//                    handlers.add(handlerClass);
////                handlerClass.newInstance();
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            });
//
//            return new Result(managerClass, handlers);
//        }
//
//        private static class Result {
//            public final Class<?> beanClass;
//            public final List<Class<?>> handlersClasses;
//
//            public Result(Class<?> beanClass, List<Class<?>> handlersClasses) {
//                this.beanClass = beanClass;
//                this.handlersClasses = handlersClasses;
//            }
//        }
//
//        @Retention(RetentionPolicy.RUNTIME)
//        public @interface Player {
//
//        }
//
//        public abstract static class BaseHandler implements IRemoteHandler<Serializable, Object[]> {
//
//            @Resource private IRemoteRequestAgent<Integer> remoteAgent;
//
//            public void remoteLogic(Object[] arguments) {
//                int gameId = (int) arguments[0];
//                remoteAgent.invokeAuthorized(
//                        gameId,
//                        arguments,
//                        getClass()
//                ).throwIfError(RuntimeException::new);
//            }
//        }
//
//        private Map<String, Class<?>> beans = new HashMap<>();
//        @Resource ConfigurableApplicationContext context;
//
//        @Override
//        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
//            Stream.of(registry.getBeanDefinitionNames())
//                    .map(registry::getBeanDefinition)
//                    .filter(definition -> definition.getBeanClassName() != null)
//                    .forEach(definition -> {
//                        try {
//                            Class<?> clazz = Class.forName(definition.getBeanClassName());
//                            Result result = generate(clazz);
//                            if (result != null) {
//                                definition.setBeanClassName(result.beanClass.getName());
//
//                                result.handlersClasses.forEach(handlerClass -> {
//                                    RootBeanDefinition handlerBeanDef = new RootBeanDefinition(handlerClass);
//                                    handlerBeanDef.setTargetType(handlerClass);
//                                    handlerBeanDef.setRole(BeanDefinition.ROLE_APPLICATION);
//                                    registry.registerBeanDefinition(handlerClass.getName(), handlerBeanDef);
//                                });
//                            }
//                        } catch (Exception e) {
//                            throw new RuntimeException(e);
//                        }
//                    });
//        }
//
//        @Override
//        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
//
//        }
//
//        public static class SomeManager {
//
//            @Player
//            public void remoteAction1(int gameId, int level) {
//                System.out.println(level);
//            }
//
//            @Player
//            void someOtherAction(int gameId, String message) {
//                System.out.println(message);
//            }
//
//            public void anotherAction1(String m) {
//                System.out.println(m);
//            }
//
//            public String anotherAction2() {
//                return "boom";
//            }
//        }
//
//        public static class Handler implements IRemoteHandler<Serializable, Serializable> {
//
//            @Override
//            public Serializable execute(Serializable request) throws Exception {
//                return null;
//            }
//        }
//    }
//
//==================================================================
//
//    @HandlerGenerator.Player
//    public void doRemote(int gameId, String message) {
//        System.out.println(message + getHero().getName());
//    }
//
//
//
//
//
//}
