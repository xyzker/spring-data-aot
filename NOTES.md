# Bad find names 

2025-11-18T09:31:46.432-05:00 ERROR 4245 --- [spring-data-aot] [           main] o.s.d.r.a.generate.AotRepositoryCreator  : Failed to contribute Repository method [dev.danvega.coffee.coffee.CoffeeRepository.findByNaame]

org.springframework.data.mapping.PropertyReferenceException: No property 'naame' found for type 'Coffee'; Did you mean 'name'
at org.springframework.data.mapping.SimplePropertyPath.<init>(SimplePropertyPath.java:94) ~[spring-data-commons-4.0.0-RC1.jar:4.0.0-RC1]
at org.springframework.data.mapping.SimplePropertyPath.create(SimplePropertyPath.java:361) ~[spring-data-commons-4.0.0-RC1.jar:4.0.0-RC1]
at org.springframework.data.mapping.SimplePropertyPath.create(SimplePropertyPath.java:336) ~[spring-data-commons-4.0.0-RC1.jar:4.0.0-RC1]
at org.springframework.data.mapping.SimplePropertyPath.lambda$from$0(SimplePropertyPath.java:289) ~[spring-data-commons-4.0.0-RC1.jar:4.0.0-RC1]
at java.base/java.util.concurrent.ConcurrentMap.computeIfAbsent(ConcurrentMap.java:330) ~[na:na]



The way AOT processing is set up is that we back off from contributing a method if an exception happens during AOT processing


The primary means of catching typos is having integration tests.


AOT repos have joined rather late the AOT party, we didn't want unreasonable exceptions to break AOT processing so we back off from method contribution. From a different perspective, there are several variants of query methods that we do not want or cannot contribute because that would be rather a overkill or we'd have to basically fall into the same reflection scheme rendering those AOT methods useless. Therefore, skipping a method on error falls into the same category of having those methods being implemented by reflection during runtime instead of having those AOT implemented.


https://docs.spring.io/spring-data/commons/reference/aot.html