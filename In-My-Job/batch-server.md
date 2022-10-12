# 배치 서버 개발기 TBD
회사 배치 서버 리뉴얼을 맡게되며 처음으로 Spring Batch와 Kotlin을 실무에서 사용했다. 이 과정들을 정리해보려고 한다. (대부분이 Spring Batch 관련 내용이 될 예정이다.)

## 목적
우리 팀에서는 다양한 데이터들을 삭제하지 않고 있다. 그 대신 delete 플래그를 이용해 처리해왔다. 

청크사이즈 이슈1 [](https://jojoldu.tistory.com/146?category=902551)[https://jojoldu.tistory.com/146?category=902551](https://jojoldu.tistory.com/146?category=902551)

청크사이즈 이슈2 [](https://jojoldu.tistory.com/166?category=902551)[https://jojoldu.tistory.com/166?category=902551](https://jojoldu.tistory.com/166?category=902551)

페이징 이슈 [](https://jojoldu.tistory.com/337?category=902551)[https://jojoldu.tistory.com/337?category=902551](https://jojoldu.tistory.com/337?category=902551)

runincrementer [](https://jojoldu.tistory.com/487)[https://jojoldu.tistory.com/487](https://jojoldu.tistory.com/487)

테스트 [](https://jojoldu.tistory.com/456?category=902551)[https://jojoldu.tistory.com/456?category=902551](https://jojoldu.tistory.com/456?category=902551) 

