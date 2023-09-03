## 다양한 상황에서의 spring data jpa (및 JPA) 동작 테스트

### 환경 설정 (test 수행 시 불필요)

#### launch mySQL with docker and create a test schema
```bash
./run-db.sh
```

#### run the application runner
```bash
./gradlew bootRUn
```

#### run test
```bash
./gradlew test
```