# Message Queue 와 Pub/Sub

메세지 큐와 Pub/Sub은 분산 시스템에서 2개 이상의 서비스들이 통신하는 일반적인 패턴이다.

### 1. Message Queue

메세지 큐는 Publishing 서비스 하나와 큐를 통해 통신하는 여러 개의 Consumer 서비스들로 구성되어 있다. 이런 통신 방식은 일반적으로 Publisher가 Consumer에 명령을 내리는 방법 중 하나이다. Publishing 서비스는 메세지를 큐/익스체인지에 넣고, 하나의 Consumer 서비스가 해당 메세지를 소비해 action을 취한다.

![](img/mq1.webp)
그림을 보면 Publisher가 새로운 메세지를 큐에 넣고있으며 소비 대기중인 여러개의 메세지가 이미 큐에 존재하는 것을 볼 수 있다. 오른쪽에는 메세지 큐를 수신중인 Consumer A와 B가 있다.

![](img/mq2.webp)

큐에 다음 메세지가 Push 되었다. 더 중요한 부분은 Comsumer A가 m1을 읽었으며 Consumer B는 해당 메세지를 더이상 큐로부터 이용할 수 없다는 것이다.

### 2. 어디에 Message Queue를 사용하는가?

메세지 큐는 서비스에서 작업을 위임하기 위해 사용된다. 그렇게 함으로써, 작업이 단 한번 수행되는 것을 보장할 수 있기 때문이다.

이는 MSA에서 대중적으로 사용되며 클라우드 기반 / 서버리스 애플리케이션을 개발하는 동안 앱을 수평적으로 확장할 수 있도록 한다.

ex) 큐에 메세지가 많은경우 이를 구독하는 Consumer들을 추가하고, 메세지가 처리된 이후에 다시 서비스를 내릴 수 있음.

### 2.1 RabbitMQ 예제

피자 레스토랑 서비스가 있다고 가정한다. 고객이 앱을 통해 피자를 주문하고, 피자 가게의 요리사들은 주문을 받는다. 여기에서 Publisher는 고객이고 요리사는 Consumer이다.

```java
private static final String MESSAGE_QUEUE = "pizza-message-queue";

@Bean
public Queue queue() {
    return new Queue(MESSAGE_QUEUE);
}
```

Spring AMQP를 사용해서, `pizza-message-queue`라는 큐를 생성했다. 다음으로 이 큐에 메세지를 post하는 Publisher를 정의한다.

```java
public class Publisher {

    private RabbitTemplate rabbitTemplate;
    private String queue;

    public Publisher(RabbitTemplate rabbitTemplate, String queue) {
        this.rabbitTemplate = rabbitTemplate;
        this.queue = queue;
    }

    @PostConstruct
    public void postMessages() {
        rabbitTemplate.convertAndSend(queue, "1 Pepperoni");
        rabbitTemplate.convertAndSend(queue, "3 Margarita");
        rabbitTemplate.convertAndSend(queue, "1 Ham and Pineapple (yuck)");
    }
}
```

Spring AMQP는 configuration 오버헤드를 줄이기 위해 우리의 RabbitMQ exchange에 연결된 RabbitTemplate Bean을 생성할 것이다. Publiser는 queue에 3개의 메세지를 보냄으로써 이를 사용한다. 주문이 들어 왔으니 이제 별도의 Consumer 애플리케이션이 필요하다.

```java
public class Consumer {
    public void receiveOrder(String message) {
        System.out.printf("Order received: %s%n", message);
    }
}
```

다음으로 reflection을 이용해 Consumer의 주문 수신 메서드를 호출하는 큐에 대한 *MessageListenerAdapter*를 만든다.

```java
@Bean
public SimpleMessageListenerContainer container(ConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter) {
    SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.setQueueNames(MESSAGE_QUEUE);
    container.setMessageListener(listenerAdapter);
    return container;
}

@Bean
public MessageListenerAdapter listenerAdapter(Consumer consumer) {
    return new MessageListenerAdapter(consumer, "receiveOrder");
}
```

큐로부터 읽은 메세지는 Consumer 클래스의 receiveOrder 메서드로 라우팅된다. 애플리케이션을 실행하기 위해 우리는 인입되는 주문을 충분히 견딜 수 있도록 Consumer를 원하는 만큼 만들 수 있다.

### 3. Pub / Sub

메세지 큐와 반대로 Pub/Sub 아키텍쳐에서는 모든 Consuming (Subscribing) 어플리케이션들이 **최소 하나**의 메세지 복제본(Publisher가 exchange로 posts한)을 얻길 원한다.

![](img/pub-sub-1.webp)

좌측을 보면 Publisher는 메세지를 Topic으로 전송한다. 이 Topic은 이 메세지를 Subscriptions로 broadcast한다. 이 subscription들은 queue들에 바인딩되어있다. 각 큐는 메세지를 대기하고 있는 listening Subscriber 서비스를 갖는다.

![](img/pub-sub-2.webp)

동일한 exchange의 시간이 흐른 뒤 모습을 살펴보자.

Subscribing 서비스 둘 다 m1을 소비한다. Topic은 새로운 메세지를 모든 Subscriber에게 분배한다.

Pub/Sub 모델은 각 Subscriber의 메세지 복사본 수신을 보장하기 위해 사용한다.

### 3.1 RabbitMQ 예제

사용자에게 Push 알림을 보내는 의류 웹사이트를 가정해보자. 시스템은 이메일 or 문자를 통해 알림을 보낼 수 있다. 이 경우 웹사이트는 Publisher, 알림 서비스는 Subscriber이다.

```java
private static final String PUB_SUB_TOPIC = "notification-topic";
private static final String PUB_SUB_EMAIL_QUEUE = "email-queue";
private static final String PUB_SUB_TEXT_QUEUE = "text-queue";

@Bean
public Queue emailQueue() {
    return new Queue(PUB_SUB_EMAIL_QUEUE);
}

@Bean
public Queue textQueue() {
    return new Queue(PUB_SUB_TEXT_QUEUE);
}

@Bean
public TopicExchange exchange() {
    return new TopicExchange(PUB_SUB_TOPIC);
}

@Bean
public Binding emailBinding(Queue emailQueue, TopicExchange exchange) {
    return BindingBuilder.bind(emailQueue).to(exchange).with("notification");
}

@Bean
public Binding textBinding(Queue textQueue, TopicExchange exchange) {
    return BindingBuilder.bind(textQueue).to(exchange).with("notification");
}
```

routing key `notification`을 이용해 2개의 큐를 바인딩했다. 이 라우팅 키와함께 전달된 모든 메세지는 듀 큐로 이동한다.

```java
rabbitTemplate.convertAndSend(topic, "notification", "New Deal on T-Shirts: 95% off!");
rabbitTemplate.convertAndSend(topic, "notification", "2 for 1 on all Jeans!");
```

### 4. 비교

위 두 모델의 이점은 동기식 통신보다 durability가 높다는 것이다. A가 B와 Http 통신한다고 가정했을 때, B가 다운된다면 데이터가 손실되고 요청을 재시도해야하지만, MQ를 사용한다면 다른 Consumer가 이를 처리할 수 있다. Pub/Sub의 경우에도 Subscriber가 다운됐을 때, 놓친 메세지를 복구하면 Subscriber Queue에서 사용할 수 있다.

두 개의 아키텍쳐를 결정하는 중요한 키는 모든 Consumer가 모든 메세지를 받는 것이 중요한 서비스인지 여부이다.
