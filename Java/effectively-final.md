# Effectively Final

> 왜 Lambda에서는 Effectively final만 사용 가능할까?
> 

### Capturing lambda

람다 표현식이 외부 스코프의 변수를 참조하는 것을 의미한다. 람다는 다음의 세 가지 변수를 캡쳐할 수 있다.

- static 변수
- instance 변수
- local 변수

람다식은 다음과 같이 동작한다.

1. 람다식은 별도의 스택을 갖는다.
2. **lambda capturing**이 일어나면 람다의 스택으로 데이터를 복사한다.

```java
Supplier<Integer> incrementer(int start) {
  return () -> start;
}
```

함수는 start라는 지역 변수를 참조하는 람다식을 반환한다. 함수가 호출되면 변수 start가 스택에 생성될테지만, 함수가 반환된 후에는 스택에서 지워진 start를 더 이상 참조할 수 없을 것이다.

**그럼에도 불구하고 이 람다식은 정상적으로 동작하는데, 그 이유가 바로 람다 스택과 값 복사가 있기 때문이다.**

### Effectively final

람다가 local 변수는 참조할 때는 final, 즉 상수여야 한다는 제한사항이 있다. 

이는 단순히 final 키워드를 선언한 변수 뿐만 아니라, 참조가 변하지 않는 경우를 포함한다. 컴파일러는 final 키워드가 없더라도 참조가 변하지 않는 상황을 파악할 수 있으며 이러한 변수를 effectively final이라 한다.

다음의 경우를 보자.

```java
Supplier<Integer> incrementer(int start) {
  return () -> start++;
}
```

이 람다식은 컴파일 에러가 발생한다. effectively final이 아니기 때문이다.

lambda capturing에서 값을 복사해오는데, 왜 변조는 하지 못하는 것일까? 그건 이 람다식이 언제 몇 개의 스레드에서 사용될 지 모르기 때문이다. 동시에 여러 스레드가 람다식을 사용하면 복사된 값의 sync를 보장할 수 없다.

다음은 람다 스코프 밖에서 변수를 수정하는 경우이다.

```java
public void localVariableMultithreading() {
    boolean run = true;
    executor.execute(() -> {
        while (run) {
            // do operation
        }
    });
    
    run = false;
}
```

while이 사용하고 있는 run의 변경을 람다 안에서는 알 방법이 없다. 하지만 사용자는 이를 신경쓰지 않아도 된다. 자바는 run 변수가 effectively final이 아니라는 것을 알고, 알아서 컴파일 에러를 내주기 때문이다.