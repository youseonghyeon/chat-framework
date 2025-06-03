## TODO list

 - 접근 제어자 정리
 - Session은 각 서버가 공유할 수 없으니, Kafka 큐를 이용하여 서버 간 통신
    - 중복 발송을 막기 위해 각 서버마다 UUID를 발급하여 Filtering
 - Room 관리용 인터페이스 생성(Redis를 대상으로 함)
 - 채팅 내역 저장소 인터페이스 생성(MySQL 또는 DynamoDB를 대상으로 함)
 - outputStream 발송 실패 예외처리 구현 필요
 - 
