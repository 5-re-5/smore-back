# Java 21 기반 이미지 사용 (Alpine 기반으로 작고 빠름)
FROM openjdk:21-jdk-slim

# 작업 디렉토리 설정
WORKDIR /app

# JAR 파일 복사 (Gradle 빌드 결과물)
COPY build/libs/*.jar app.jar

# 컨테이너 시작 시 실행할 명령
ENTRYPOINT ["sh", "-c", "java -jar app.jar"]

# 애플리케이션 포트 (필요 시 변경)
EXPOSE 8081
