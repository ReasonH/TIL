# 상속 다루기

상속은 아주 유용한 동시에 오용하기 쉽다.

### 12.1 메서드 올리기

무언가 중복되었다는 것은 한쪽의 변경이 다른 쪽에는 반영되지 않을 수 있다는 위험을 항상 수반한다. 그러나 일반적으로는 중복을 찾기가 쉽지 않다.

메서드들의 본문 코드가 똑같을 때는 메서드 올리기를 적용하기 가장 쉬운 상황이지만, 항상 이처럼 만만하지는 않다. 차이점을 찾는 방법은 테스트에서 놓친 동작까지 알게 해주는 경우가 자주 있다.

- 서로 다른 클래스의 두 메서드를 각각 매개변수화하면 궁극적으로 같은 메서드가 되기도 한다.
- 반면, 메서드의 본문에서 참조하는 필드들이 서브클래스에만 있는 경우, 필드를 먼저 슈퍼클래스로 올린 후에 메서드로 올려야 한다.

**절차**

1. 똑같이 동작하는 메서드인지 살펴본다.
   1. 실질적으로 하는 일이 동일하고 코드가 다르다면 본문 코드가 같아질 때까지 리팩터링
2. 메서드 안에서 호출하는 **다른 메서드 / 참조하는 필드**들을 슈퍼클래스에서도 호출하고 참조할 수 있는지 확인
3. 메서드 시그니처가 다르다면 함수 선언 바꾸기로 슈퍼클래스에서 사용하고 싶은 형태로 통일
4. 슈퍼클래스에 새로운 메서드를 생성하고 대상 메서드 코드를 복사
5. 정적 검사
6. 서브클래스 중 하나의 메서드 제거
7. 테스트
8. 모든 서브클래스 메서드가 없어질 때까지 서브클래스의 다른 메서드를 하나씩 제거

### 12.2 필드 올리기

서브클래스들이 독립적으로 개발된 경우 일부 기능이 중복되어 있을 때가 있다. 특히, 필드가 중복되기 쉽다. 필드가 비슷한 방식으로 쓰인다고 판단되면 슈퍼클래스로 끌어올린다.

**절차**

1. 후보 필드들을 사용하는 곳 모두가 필드를 동일한 방식으로 사용하는지 살핀다.
2. 필드들 이름이 각기 다르다면 똑같은 이름으로 변경
3. 슈퍼클래스에 새로운 필드 생성
4. 서브클래스의 필드 제거
5. 테스트

### 12.3 생성자 본문 올리기

생성자는 할 수 있는 일과 호출 순서에 제약이 있기에 리팩터링 시 조금 다른 방식으로 접근해야 한다.

**절차**

1. 슈퍼클래스에 생성자가 없다면 하나 정의, 서브클래스의 생성자들에서 이 생성자(super()) 호출되는지 확인

   ```java
   class Employee extends Party {
   	constructor(name, id) {
   		super();
   		this._id = id;
   		this._name = name;
   	}
   }

   class Department extends Party {
   	constructor(name, staff) {
   		super();
   		this._id = id;
   		this._staff = staff;
   	}
   }
   ```

2. 문장 슬라이드하기로 공통 문장 모두를 super() 호출 직후로 옮긴다.

   ```java
   class Employee extends Party {
   	constructor(name, id) {
   		super();
   		**this._name = name;**
   		this._id = id;
   	}
   }

   class Department extends Party {
   	constructor(name, staff) {
   		super();
   		this._name = name;
   		this._staff = staff;
   	}
   }
   ```

3. 공통 코드를 슈퍼클래스에 추가하고 서브클래스들에서는 제거한다. 생성자 매개변수 중 공통 코드에서 참조하는 값들을 모두 super()로 전달

   ```java
   **class Party {
   	constructor(name) {
   		this._name = name;
   	}
   }**

   class Employee extends Party {
   	constructor(name, id) {
   		**super(name);**
   		this._id = id;
   	}
   }

   class Department extends Party {
   	constructor(name, staff) {
   		**super(name);**
   		this._staff = staff;
   	}
   }
   ```

4. 테스트
5. 생성자 시작 부분으로 옮길 수 없는 코드는 함수 추출하기와 메서드 올리기를 차례로 적용

### 12.4 메서드 내리기

특정 서브클래스와만 관련된 메서드는 슈퍼클래스에서 제거하고 해당 서브 클래스에 추가하는게 깔끔하다. 다만 호출자가 해당 기능을 제공하는 서브클래스를 정확히 알지 못하는 상황이라면 슈퍼클래스의 조건부 로직을 다형성으로 바꿔야 한다.

### 12.5 필드 내리기

서브클래스에서만 사용되는 필드는 해당 서브클래스로 옮긴다.

### 12.6 타입 코드를 서브클래스로 바꾸기

↔ 반대 리팩터링은 서브클래스 제거하기

SW는 비슷한 대상들을 특정 특성에 따라 구분해야 할 때가 자주 있다. 이를 다루는 수단으로는 타입 코드 필드가 있다. 타입 코드만으로 부족한 상황에서는 서브클래스를 사용한다.

1. 타입 코드에 따라 동작이 달라져야 하는 함수가 여러 개일 때
2. 특정 타입에서만 의미가 있는 값을 사용하는 필드나 메서드가 있을 때

서브클래스의 장점이 나타난다.

이 리팩터링은 대상 클래스에 직접 적용할지, 아니면 타입 코드 자체에 적용할지를 고민해야 한다. 전자의 경우 타입 코드를 다른 용도로 사용할 수 없다는 단점이 있다.

**절차**

1. 타입 코드 필드를 자가 캡슐화한다.
2. 타입 코드 값 하나를 선택하여 그 값에 해당하는 서브클래스를 만든다. 타입 코드 게터 메서드를 오버라이드하여 해당 타입 코드 리터럴 값을 반환하게 한다.
3. 매개변수로 받은 타입 코드와 방금 만든 서브클래스를 매핑하는 선택 로직을 만든다. (팩터리 함수 등)
4. 테스트
5. 타입 코드 값 각각에 서브클래스 생성과 선택 로직 추가를 반복, 클래스 하나가 완성될 때마다 테스트
6. (캡슐화된) 타입 코드 필드를 제거
7. 테스트
8. 타입 코드 접근자를 이용하는 메서드 모두에 메서드 내리기와 조건부 로직을 다형성으로 바꾸기 적용

### 12.7 서브클래스 제거하기

더 이상 쓰이지 않는 서브클래스는 슈퍼클래스의 필드로 대체해 제거하는 게 최선이다.

**절차**

1. 서브클래스 생성자를 팩터리 함수로 변경
2. 서브클래스 타입을 검사하는 코드가 있다면 검사 코드에 함수 추출하기와 함수 옮기기를 적용하여 슈퍼클래스로 이동한다.
3. 서브클래스 타입을 나타내는 필드를 슈퍼클래스에 만든다.
4. 서브클래스를 참조하는 메서드가 방금 만든 타입 필드를 이용하도록 수정한다.
5. 서브클래스 삭제

### 12.8 슈퍼클래스 추출하기

비슷한 일을 수행하는 두 클래스가 보이면 상속 메커니즘을 이용해서 비슷한 부분을 공통 슈퍼클래스로 옮겨담을 수 있다. 상속은 프로그램이 성장하며 깨우치게 된다. 클래스 추출하기와 슈퍼클래스 추출하기는 해결 방식의 차이일 뿐이지만, 일반적으로 슈퍼클래스 추출이 간단하므로 이 리팩터링을 먼저 시도해보자.

**절차**

1. 빈 슈퍼클래스를 만든다. 원래 클래스들이 새 클래스를 상속하도록 한다.
2. 테스트
3. 생성자 본문 올리기, 메서드 올리기, 필드 올리기를 적용하여 공통 원소를 슈퍼클래스로 옮긴다.
4. 서브클래스에 남은 메서드들을 검토한다. 공통 부분이 있으면 함수로 추출한 다음 메서드 올리기를 적용한다.
5. 원래 클래스들을 사용하는 코드를 검토하여 슈퍼클래스 인터페이스를 사용하게 할지 고민해본다.

### 12.9 계층 합치기

클래스 계층 구조를 리팩터링하다 보면 기능들을 위로 올리거나 아래로 내리는 일이 다반사로 벌어진다. 어떤 클래스와 그 부모가 너무 비슷해져서 더는 독립적으로 존재해야 할 이유가 사라진다면 그 둘을 하나로 합쳐야 할 시기이다.

**절차**

1. 두 클래스 중 제거할 것을 고른다.
2. 필드 올리기(or 내리기), 메서드 올리기(or 내리기)를 적용하여 모든 요소를 하나의 클래스로 옮긴다.
3. 제거할 클래스를 참조하던 모든 코드가 남겨질 클래스를 참조하도록 고친다.
4. 빈 클래스를 제거한다.
5. 테스트

### 12.10 서브클래스를 위임으로 바꾸기

상속에는 단점이 있다. 무언가가 달라져야 하는 이유가 여러 개여도 상속은 그중 단 하나의 이유만 선택해 기준으로 삼을 수밖에 없다. 사람 객체의 동작을 ‘나이대'와 ‘소득 수준'에 따라 달리 하고 싶다면 서브클래스는 젊은이와 어르신이 되거나, 부자와 서민이 되어야 한다. 둘 다는 안 된다.

또 다른 문제는 클래스들의 관계를 강하게 결합시킨다는 점이다. 부모를 수정하면 이미 존재하는 자식들의 기능을 해치기가 쉽기 때문에 각별히 주의해야 한다.

위임은 두 문제를 모두 해결해준다. 위임은 객체 사이 일반적 관계이므로 상호작용에 필요한 인터페이스를 명확히 정의할 수 있다. 즉 상속보다 결합도가 훨씬 약하다.

**절차**

1. 생성자를 호출하는 곳이 많다면 생성자를 팩터리 함수로 바꾼다.
2. 위임으로 활용할 빈 클래스를 만든다. 서브클래스에 특화된 데이터를 전부 받아야 하며 보통은 슈퍼클래스를 가리키는 역참조도 필요하다.
3. 위임을 저장할 필드를 슈퍼클래스에 추가
4. 서브클래스 생성 코드를 수정하여 위임 인스턴스를 생성하고 위임 필드에 대입해 초기화
5. 서브클래스 메서드 중 위임 클래스로 이동할 것을 고른다.
6. 함수 옮기기를 적용해 위임 클래스로 옮긴다. 원래 메서드에서 위임하는 코드는 지우지 않는다.
7. 서브클래스 외부에도 원래 메서드를 호출하는 코드가 있다면 서브클래스의 위임 코드를 슈퍼클래스로 옮긴다. 이때 위임이 존재하는지를 검사하는 보호 코드로 감싸야 한다. 호출하는 외부 코드가 없다면 원래 메서드는 죽은 코드가 되므로 제거한다.
8. 테스트
9. 서브클래스의 모든 메서드가 옮겨질 때까지 5~8 과정을 반복한다.
10. 서브클래스들의 생성자를 호출하는 코드를 찾아서 슈퍼클래스의 생성자를 사용하도록 수정한다.
11. 테스트
12. 서브클래스 삭제

### 12.10 예시

```jsx
class Booking {
  constructor(show, date) {
    this._show = show;
    this._date = date;
  }
  get hasTalkback() {
    return this._show.hasOwnProperty("talkback") && !this.isPeakDay;
  }
  get basePrice() {
    let result = this._show_price;
    if (this.isPeakDay) result += Math.round(result * 0.15);
    return result;
  }
}

class PremiumBooking extends Booking {
  constructor(show, date, extras) {
    super(show, date);
    this._extras = extras;
  }
  get hasTalkback() {
    return this._show.hasOwnProperty("talkback");
  }
  get basePrice() {
    return Math.round(super.basePrice + this._extras.premiumFee);
  }
  get hasDinner() {
    return this._extras.hasOwnProperty("dinner") && !this.isPeakDay;
  }
}
```

상속은 한 번만 사용할 수 있는 도구다. 상속을 사용해야 할 다른 이유가 생기고, 그 이유가 프리미엄 예약 서브클래스보다 가치가 크다고 생각된다면 프리미엄 예약을 상속이 아닌 다른 방식으로 표현해야 할 것이다. 다음과 같이 두 예약 클래스의 생성자를 호출하는 클라이언트들이 있다고 가정하자.

```jsx
// client 1
aBooking = new Booking(show, date);

// client 2
aBooking = new PremiumBooking(show, date, extras);
```

1. 먼저 생성자를 팩터리 함수로 바꿔서 생성자 호출 부분을 캡슐화한다.

   ```jsx
   function createBooking(show, date) {
     return new Booking(show, date);
   }
   function createPremiumBooking(show, date, extras) {
     return new PremiumBooking(show, date, extras);
   }

   // client 1
   aBooking = createBooking(show, date);

   // client 2
   aBooking = createPremiumBooking(show, date, extras);
   ```

2. 위임클래스를 새로 만든다. 위임 클래스의 생성자는 서브클래스에서만 사용하던 매개변수와 예약 객체로의 역참조를 매개변수로 받는다. 역참조가 필요한 이유는 서브클래스 메서드 중 슈퍼클래스에 저장된 데이터를 사용하는 경우가 있기 때문이다. 상속과 다르게 위임은 역참조가 있어야 한다.

   ```jsx
   class PremiumBookingDelegate {
     constructor(hostBooking, extras) {
       this._host = hostBooking;
       this._extras = extras;
     }
   }
   ```

3. 이제 새로운 위임을 예약 객체와 연결한다.

   ```jsx
   // Booking
   _bePremium(extras) {
   	this._premiumDelegate = new PremiumBookingDelegate(this, extras);
   }

   function createPremiumBooking(show, date, extras) {
   	const result = new PremiumBooking(show, date, extras);
   	result._bePremium(extras);
   	return result;
   }
   ```

4. 다음은 기능을 옮길 차례다. 가장 먼저 hasTalkback()을 고민해보자

   ```jsx
   // Booking
   get hasTalkback() {
   	return this._show.hasOwnProperty('talkback') && !this.isPeakDay;
   }

   // PremiumBooking
   get hasTalkback() {
   	return this._show.hasOwnProperty('talkback');
   }
   ```

5. 함수 옮기기를 적용해 서브클래스 메서드를 위임으로 옮긴다. 슈퍼클래스 데이터를 사용하는 부분은 \_host를 통하도록 고친다.

   ```jsx
   class PremiumBookingDelegate {
     constructor(hostBooking, extras) {
       this._host = hostBooking;
       this._extras = extras;
     }
     get hasTalkback() {
       return this._host._show.hasOwnProperty("talkback");
     }
   }
   ```

6. 서브클래스(PremiumBooking)의 메서드를 삭제한다.
7. 위임을 사용하는 분배 로직을 슈퍼클래스 메서드에 추가

   ```jsx
   // Booking
   get hasTalkback() {
   	return (this._premiumDelegate)
   		? this._premiumDelegate.hasTalkback
   		: this._show.hasOwnProperty('talkback') && !this.isPeakDay;
   }
   ```

8. 반복…
9. 서브클래스의 동작을 모두 옮겼다면 팩터리 메서드가 슈퍼클래스를 반환하도록 수정한 뒤 서브클래스를 삭제

   ```jsx
   function createPremiumBooking(show, date, extras) {
     const result = new Booking(show, date, extras);
     result._bePremium(extras);
     return result;
   }
   ```

### 12.11 슈퍼클래스를 위임으로 바꾸기

상속은 기존 기능을 재활용하는 강력하고 손쉬운 수단이다. 그러나 상속이 혼란과 복잡도를 키우는 방식으로 이뤄지기도 한다. 슈퍼클래스의 기능들이 서브클래스에는 어울리지 않는다면 그 기능들을 상속을 통해 이용하면 안된다는 신호다. 제대로 된 상속은 슈퍼클래스가 사용되는 모든 곳에서 서브클래스의 인스턴스를 사용해도 이상없이 동작해야 한다.

잘못된 상속 사용에서 오는 혼란은 위임을 통한 객체 분리를 통해 손쉽게 피할 수 있다.

**절차**

1. 슈퍼클래스 객체를 참조하는 필드(위임 참조)를 서브클래스에 만든다. 위임 참조를 새로운 슈퍼클래스 인스턴스로 초기화한다.
2. 슈퍼클래스 동작 각각에 대응하는 전달 함수를 서브클래스에 만든다. 서로 관련된 함수끼리 그룹으로 묶어 진행하며, 한 그룹마다 테스트
3. 슈퍼클래스 동작 모두가 전달 함수로 오버라이드 되었다면 상속 관계를 끊는다.

```jsx
class CatalogItem {
  constructor(id, title, tags) {
    this._id = id;
    this._title = title;
    this._tags = tags;
  }

  get id() {
    return this._id;
  }
  get title() {
    return this._title;
  }
  hasTag(arg) {
    return this._tags.includes(arg);
  }
}

class Scroll extends CatalogItem {
  constructor(id, title, tags, dateLastCleaned) {
    super(id, title, tags);
    this._lastCleaned = dateLastCleaned;
  }

  needsCleaning(targetDate) {
    const threshold = this.hasTag("reserved") ? 700 : 1500;
    return this.daySinceLastCleaning(targetDate) > threshold;
  }

  daySinceLastCleaning(targetDate) {
    return this._lastCleaned.until(targetDate, ChronoUnit.DAYS);
  }
}
```

이는 흔한 모델링 실수의 예다. 스크롤은 사본이 여러개임에도 카탈로그 아이템은 하나일 수 있다. 이런 경우 사본 중 하나를 수정한다면 다른 사본들의 올바른 수정 여부까지 주의해서 확인해야 한다. (이런 문제가 아니더라도 향후 혼란을 야기할 수 있기 때문에 이 관계를 끊는게 좋다.)

1. 먼저 Scroll에 카탈로그 아이템을 참조하는 속성을 만들고 슈퍼클래스 인스턴스를 새로 만들어 대입한다.

   ```jsx
   class Scroll extends CatalogItem {
   	constructor(id, title, tags, dateLastCleaned) {
   		super(id, title, tags);
   		**this._catalogItem = new CatalogItem(id, title, tags);**
   		this._lastCleaned = dateLastCleaned;
   	}
   }
   ```

2. 서브클래스에서 사용하는 슈퍼클래스 동작 각각에 대응하는 전달 메서드를 만든다.

   ```jsx
   class Scroll extends CatalogItem {
   	constructor(id, title, tags, dateLastCleaned) {
   		super(id, title, tags);
   		this._catalogItem = new CatalogItem(id, title, tags);
   		this._lastCleaned = dateLastCleaned;
   	}
   	**get id() {return this._catalogItem.id;}
   	get title() {return this._catalogItem.title;}
   	hasTag(aString) {return this._catalogItem.hasTag(aString);}**
   }
   ```

3. 상속 관계를 끊는다.

   ```jsx
   class Scroll {
     constructor(id, title, tags, dateLastCleaned) {
       super(id, title, tags);
       this._catalogItem = new CatalogItem(id, title, tags);
       this._lastCleaned = dateLastCleaned;
     }
     get id() {
       return this._catalogItem.id;
     }
     get title() {
       return this._catalogItem.title;
     }
     hasTag(aString) {
       return this._catalogItem.hasTag(aString);
     }
   }
   ```

여기까지가 리팩터링의 끝이지만, 스크롤들이 각각의 카탈로그 인스턴스를 갖게 되는 문제가 있다. 여기에서는 값을 참조로 바꾸는 리팩터링을 한번 더 수행할 수 있다.
