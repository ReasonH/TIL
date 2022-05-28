# Retention

아주 가끔 필요한 어노테이션을 만들어서 사용해야하는 경우가 있다. 이 때 Retention을 그냥 `RUNTIME`으로 지정하곤 했다. (이러면 최대 범위이기 때문에 문제가 될 일이 없다.) 오늘은 이를 제대로 정리하고 앞으로 사용 시, 올바르게 인지한 상태에서 사용하려 한다.

다음은 javadoc에서 설명되어 있는 Retention의 Enum에 대한 설명이다.

> **Enum Constant and Description**.  
> `CLASS`  
> Annotations are to be recorded in the class file by the compiler but need not be retained by the VM at run time.  
> `RUNTIME`  
> Annotations are to be recorded in the class file by the compiler and retained by the VM at run time, so they may be read reflectively.  
> `SOURCE`  
> Annotations are to be discarded by the compiler.

간단히 한국어로 풀어서 쓰면 다음과 같다.

- SOURCE: 컴파일 시 버려지는 어노테이션
- CLASS: 컴파일러에 의해 생성된 클래스 파일까지 유지, 런타임에서는 유지될 필요 없는 어노테이션
- RUNTIME: 컴파일러에 의해 생성된 클래스 파일부터 런타임에까지 남아있어 리플렉션될 수 있는 어노테이션

각 Retention을 예시와 함께 살펴보자.

### SOURCE

ex) lombok의 `@Getter`, `@Setter`

java 개발에서 자주 사용되는 lombok 라이브러리에서 제공되는 어노테이션이다. 이 어노테이션들은 컴파일 타임에 별도의 get / set 코드를 생성해서 넣어주는 역할을 수행한다. 해당 어노테이션을 붙이고 컴파일한 class 파일을 살펴보면 어노테이션은 찾아볼 수 없다. 어노테이션 자체는 코드 생성을 위임한다는 플래그 역할만 하기 때문에 바이트코드에서까지 유지될 필요가 없기 때문이다.

### CLASS

ex) lombok의 `@NotNull`

이 어노테이션은 **메서드의 파라미터**에 붙인 경우 해당 파라미터를 사용하는 메서드 시작부분에 null 체크 로직을 삽입하며 **필드에 붙인 경우** 필드에 값을 할당하는 메서드에 null 체크 로직을 삽입해준다. 얼추 보면 컴파일 타임에 로직을 삽입하는 역할이 위의 `@Getter`, `@Setter`와 유사해 보인다. 그러나 해당 어노테이션은 컴파일 뒤의 class 파일에도 남아있는 것을 확인할 수 있다. 이유가 무엇일까?

### SOURCE vs CLASS

maven이나 gradle로 외부 의존성을 불러오는 경우 일반적으로 jar파일을 로딩하게 된다. 이 때 jar파일은 원본 source 파일이 아닌 class 파일만 포함되어 있다. **intelliJ 등의 IDE는 어노테이션을 이용한 정적 분석 기능 같은 부가 기능을 제공하는 경우가 있는데, class 파일에 어노테이션이 포함되어 있지 않다면 외부 의존성을 통한 class에서는 이를 이용할 수 없게 되는 것이다.** 이외에도 클래스로딩 후 어노테이션을 통한 별도의 작업을 위해서는 CLASS Retention이 필요하다.

### RUNTIME

ex) spring의 `@Service`, `@Autowire` 등

이 어노테이션은 스프링의 Bean 설정 및 주입을 위해 사용한다. 스프링은 런타임에서 컴포넌트 스캔으로 Bean을 등록하고, 의존성 주입 등을 수행한다. 따라서 이 어노테이션은 런타임에도 살아남아 어떤 class가 Bean이 되고, 어떤 필드에 Bean이 주입되어야 하는지 스프링에게 알려주어야 한다. 최종적으로 스프링 컨테이너는 Reflection등을 활용해 이런 어노테이션이 붙은 class들을 활용하게 된다.
